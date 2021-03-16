package repositories

import java.sql.SQLIntegrityConstraintViolationException
import java.time.ZonedDateTime

import javax.inject.{Inject, Singleton}
import models._
import play.api.Logging
import scalikejdbc._
import support.auth.AuthUtils

import scala.util.{Failure, Success, Try}

@Singleton
class PlayerRepository @Inject()() extends Logging {

  def create(p: PlayerCreationCommand)(implicit session: DBSession): Either[AppError, Player] = Try {
    PlayerRepository.create(p.username, AuthUtils.hash(p.password))
  } match {
    case Success(value) => Right(value)
    case Failure(e: SQLIntegrityConstraintViolationException) =>
      logger.error("Duplicate player username", e)
      Left(NotUniqueError("Another player with same username already exists"))
    case Failure(e) =>
      logger.error("Error while saving player", e)
      Left(UnexpectedError("Unexpected error"))
  }

  def deactivate(id: Long)(implicit session: DBSession): Either[AppError, Unit] = Try {
    PlayerRepository.deactivate(id)
  } match {
    case Success(_) => Right(())
    case Failure(e) =>
      logger.error("Error while deactivating player", e)
      Left(UnexpectedError("Unexpected error"))
  }

  def findById(id: Long)(implicit session: DBSession): Either[AppError, Player] = Try {
    PlayerRepository.findById(id)
  } match {
    case Success(Some(value)) => Right(value)
    case Success(None) => Left(ResourceNotFound(s"player with $id not found"))
    case Failure(e) =>
      logger.error("Error while finding player", e)
      Left(UnexpectedError("Unexpected error"))
  }

  def findByCredentials(user: String, encodedPass: String)(implicit session: DBSession): Either[AppError, Player] = Try {
    PlayerRepository.findByCredentials(user, encodedPass)
  } match {
    case Success(Some(value)) => Right(value)
    case Success(None) => Left(ResourceNotFound(s"player $user not found"))
    case Failure(e) =>
      logger.error("Error while finding player", e)
      Left(UnexpectedError("Unexpected error"))
  }

  //  def exists(user: String, encodedPass: String)(implicit session: DBSession): Either[AppError, Boolean] = Try {
  //    PlayerRepository.findById(id)
  //  } match {
  //    case Success(Some(_)) => Right(true)
  //    case Success(None) => Right(false)
  //    case Failure(e) =>
  //      logger.error("Error while finding player", e)
  //      Left(UnexpectedError("Unexpected error"))
  //  }

}

object PlayerRepository extends SQLSyntaxSupport[Player] {

  // If the table name is same as snake_case'd name of this companion object, you don't need to specify tableName explicitly.
  override val tableName = "player"
  // By default, column names will be cached from meta data automatically when accessing this table for the first time.
  override val columns = Seq("id", "username", "encoded_pass", "created_at", "deleted_at")

  // simple extractor
  def apply(p: SyntaxProvider[Player])(rs: WrappedResultSet): Player = apply(p.resultName)(rs)

  def apply(p: ResultName[Player])(rs: WrappedResultSet): Player = new Player(
    id = rs.get(p.id),
    username = rs.get(p.username),
    encodedPass = rs.get(p.encodedPass),
    createdAt = rs.get(p.createdAt),
    deletedAt = rs.get(p.deletedAt))

  def opt(s: SyntaxProvider[Player])(rs: WrappedResultSet): Option[Player] = rs.longOpt(s.resultName.id).map(_ => apply(s.resultName)(rs))

  // SyntaxProvider objects
  val p = PlayerRepository.syntax("p")

  // reusable part of SQL
  val isNotDeleted: SQLSyntax = sqls.isNull(p.deletedAt)

  // find by primary key
  def findById(id: Long)(implicit session: DBSession): Option[Player] = withSQL {
    select.from(PlayerRepository as p).where.eq(p.id, id).and.append(isNotDeleted)
  }.map(PlayerRepository(p)).single.apply()

  // find by primary key
  def findByCredentials(username: String, encodedPass: String)(implicit session: DBSession): Option[Player] = withSQL {
    select.from(PlayerRepository as p).where.eq(p.username, username).and.eq(p.encodedPass, encodedPass).and.append(isNotDeleted)
  }.map(PlayerRepository(p)).single.apply()

  def create(username: String, encodedPass: String, createdAt: ZonedDateTime = ZonedDateTime.now)(implicit session: DBSession): Player = {
    val id = withSQL {
      insert.into(PlayerRepository).namedValues(
        column.username -> username,
        column.encodedPass -> encodedPass,
        column.createdAt -> createdAt)
    }.updateAndReturnGeneratedKey.apply()

    Player(
      id = id,
      username = username,
      encodedPass = encodedPass,
      createdAt = createdAt)
  }

  def deactivate(id: Long)(implicit session: DBSession): Unit = withSQL {
    update(PlayerRepository).set(column.deletedAt -> ZonedDateTime.now).where.eq(column.id, id)
  }.update.apply()
}
