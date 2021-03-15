package repositories

import java.sql.ResultSet
import java.time.ZonedDateTime

import models.GameActions._
import javax.inject.{Inject, Singleton}
import models.{GameState, _}
import play.api.Logging
import scalikejdbc._

import scala.util.{Failure, Success, Try}

@Singleton
class GameRepository @Inject()() extends Logging {

  def create(cmd: GameCreationCommand, state: GameState)(implicit session: DBSession): Either[AppError, Game] = Try {
    val now = ZonedDateTime.now()
    GameRepository.create(cmd.playerId, state, now, cmd.height, cmd.width, cmd.mines, now)
  } match {
    case Success(value) => Right(value)
    case Failure(e) =>
      logger.error("Error while saving new game", e)
      Left(UnexpectedError("Unexpected error"))
  }

  def save(game: Game)(implicit session: DBSession): Either[AppError, Unit] = Try {
    GameRepository.save(game)
  } match {
    case Success(affectedRows) if affectedRows > 0 => Right(())
    case Success(affectedRows) if affectedRows <= 0 => Left(UnexpectedError("Unexpected error. No affected rows when trying to update a game"))
    case Failure(e) =>
      logger.error(s"Error while saving a game [id: ${game.id}]", e)
      Left(UnexpectedError("Unexpected error"))
  }

  def deactivate(id: Long)(implicit session: DBSession): Either[AppError, Unit] = Try {
    GameRepository.deactivate(id)
  } match {
    case Success(_) => Right(())
    case Failure(e) =>
      logger.error("Error while deactivating game", e)
      Left(UnexpectedError("Unexpected error"))
  }

  def find(id: Long)(implicit session: DBSession): Either[AppError, Game] = Try {
    GameRepository.findById(id)
  } match {
    case Success(Some(value)) => Right(value)
    case Success(None) => Left(ResourceNotFound(s"game $id not found"))
    case Failure(e) =>
      logger.error("Error while deactivating game", e)
      Left(UnexpectedError("Unexpected error"))
  }

}

object GameRepository extends SQLSyntaxSupport[Game] {

  import gameBinders._

  override val tableName = "game"
  override val columns = Seq("id", "player_id", "state", "start_time", "finish_time", "height", "width", "mines",
    "created_at", "deleted_at")

  // simple extractor
  def apply(p: SyntaxProvider[Game])(rs: WrappedResultSet): Game = apply(p.resultName)(rs)

  def apply(p: ResultName[Game])(rs: WrappedResultSet): Game = Game(
    id = rs.get(p.id),
    playerId = rs.get(p.playerId),
    state = rs.get(p.state),
    startTime = rs.get(p.startTime),
    finishTime = rs.get(p.finishTime),
    height = rs.get(p.height),
    width = rs.get(p.width),
    mines = rs.get(p.mines),
    cells = Nil,
    createdAt = rs.get(p.createdAt),
    deletedAt = rs.get(p.deletedAt))

  def opt(s: SyntaxProvider[Game])(rs: WrappedResultSet): Option[Game] = rs.longOpt(s.resultName.id).map(_ => apply(s.resultName)(rs))

  val g = GameRepository.syntax("g")
  val c = CellRepository.c

  private val isNotDeleted = sqls.isNull(g.deletedAt)

  // find by primary key
  def findById(id: Long)(implicit session: DBSession): Option[Game] = withSQL.apply[Game] {
    select.from(GameRepository as g)
      .leftJoin(CellRepository as CellRepository.c).on(c.gameId, g.id)
      .where.eq(g.id, id).and.append(isNotDeleted)
  }
    .one(GameRepository(g))
    .toMany(CellRepository.opt(c))
    .map { (game, cells) => game.copy(cells = cells) }
    .single.apply()

  def create(playerId: Long, gameState: GameState, startTime: ZonedDateTime, height: Int, width: Int, mines: Int,
             createdAt: ZonedDateTime = ZonedDateTime.now)(implicit session: DBSession): Game = {
    val id = withSQL {
      insert.into(GameRepository).namedValues(
        column.playerId -> playerId,
        column.state -> gameState,
        column.startTime -> startTime,
        column.height -> height,
        column.width -> width,
        column.mines -> mines,
        column.createdAt -> createdAt)
    }.updateAndReturnGeneratedKey.apply()

    Game(
      id = id,
      playerId = playerId,
      state = gameState,
      startTime = startTime,
      height = height,
      width = width,
      mines = mines,
      cells = List.empty,
      createdAt = createdAt)
  }

  def save(game: Game)(implicit session: DBSession): Int = {
    withSQL {
      update(GameRepository).set(
        column.playerId -> game.playerId,
        column.state -> game.state,
        column.startTime -> game.startTime,
        column.height -> game.height,
        column.width -> game.width,
        column.mines -> game.mines)
        .where.eq(column.id, game.id)
    }.update.apply()
  }

  def deactivate(id: Long)(implicit session: DBSession): Unit = withSQL {
    update(GameRepository).set(column.deletedAt -> ZonedDateTime.now).where.eq(column.id, id)
  }.update.apply()

  object gameBinders {
    implicit val gameStateTypeBinder: TypeBinder[GameState] = new TypeBinder[GameState] {
      def apply(rs: ResultSet, label: String): GameState = GameState.withNameInsensitive(rs.getString(label))

      def apply(rs: ResultSet, index: Int): GameState = GameState.withNameInsensitive(rs.getString(index))
    }
    implicit val gameStateParameterBinderFactory: ParameterBinderFactory[GameState] = ParameterBinderFactory[GameState] {
      value => (stmt, idx) => stmt.setString(idx, value.entryName)
    }
  }

}
