package repositories

import java.sql.ResultSet

import javax.inject.{Inject, Singleton}
import models._
import play.api.Logging
import scalikejdbc._

import scala.util.{Failure, Success, Try}

@Singleton
class CellRepository @Inject()() extends Logging {

  def create(cells: Seq[CellCreationCommand])(implicit session: DBSession): Either[AppError, Unit] = Try {
    CellRepository.create(cells)
  } match {
    case Success(_) => Right(())
    case Failure(e) =>
      logger.error("Error while saving new cells", e)
      Left(UnexpectedError("Unexpected error"))
  }

  def save(cell: Cell)(implicit session: DBSession): Either[AppError, Unit] = Try {
    CellRepository.save(cell)
  } match {
    case Success(affectedRows) if affectedRows > 0 => Right(())
    case Success(affectedRows) if affectedRows <= 0 => Left(UnexpectedError("Unexpected error. No affected rows when trying to update a cell"))
    case Failure(e) =>
      logger.error(s"Error while saving a cell [id: ${cell.id}]", e)
      Left(UnexpectedError("Unexpected error"))
  }
}

object CellRepository extends SQLSyntaxSupport[Cell] {

  import cellBinders._

  override val tableName = "cell"
  override val columns = Seq("id", "game_id", "x", "y", "state", "has_mine", "adjacent_mines")

  def apply(p: SyntaxProvider[Cell])(rs: WrappedResultSet): Cell = apply(p.resultName)(rs)

  def apply(p: ResultName[Cell])(rs: WrappedResultSet): Cell = new Cell(
    id = rs.get(p.id),
    gameId = rs.get(p.gameId),
    x = rs.get(p.x),
    y = rs.get(p.y),
    state = rs.get(p.state),
    hasMine = rs.get(p.hasMine),
    adjacentMines = rs.get(p.adjacentMines))

  def opt(s: SyntaxProvider[Cell])(rs: WrappedResultSet): Option[Cell] = rs.longOpt(s.resultName.id).map(_ => apply(s.resultName)(rs))

  val c = CellRepository.syntax("c")

  def create(cells: Seq[CellCreationCommand])(implicit session: DBSession): Seq[Int] = {
    val batchParams: Seq[Seq[Any]] = cells.foldLeft(Seq.empty[Seq[Any]])((seq, cell) =>
      seq :+ Seq(cell.gameId, cell.x, cell.y, CellState.Covered.toString, cell.hasMine, cell.adjacentMines))
    withSQL {
      insert.into(CellRepository).namedValues(
        column.gameId -> sqls.?,
        column.x -> sqls.?,
        column.y -> sqls.?,
        column.state -> sqls.?,
        column.hasMine -> sqls.?,
        column.adjacentMines -> sqls.?,
      )
    }.batch(batchParams: _*).apply()
  }

  def save(cell: Cell)(implicit session: DBSession): Int = {
    withSQL {
      update(CellRepository).set(
        column.x -> cell.x,
        column.y -> cell.y,
        column.state -> cell.state,
        column.hasMine -> cell.hasMine,
        column.adjacentMines -> cell.adjacentMines)
        .where.eq(column.id, cell.id)
    }.update.apply()
  }

  object cellBinders {
    implicit val cellStateTypeBinder: TypeBinder[CellState] = new TypeBinder[CellState] {
      def apply(rs: ResultSet, label: String): CellState = CellState.withNameInsensitive(rs.getString(label))

      def apply(rs: ResultSet, index: Int): CellState = CellState.withNameInsensitive(rs.getString(index))
    }
    implicit val gameStateParameterBinderFactory: ParameterBinderFactory[CellState] = ParameterBinderFactory[CellState] {
      value => (stmt, idx) => stmt.setString(idx, value.entryName)
    }
  }

}
