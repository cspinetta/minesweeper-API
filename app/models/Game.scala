package models

import java.time.ZonedDateTime

import enumeratum._

import scala.collection.immutable

object GameActions {

  case class GameCreationCommand(playerId: Long, height: Int, width: Int, mines: Int)

  case class RevealCellCommand(x: Int, y: Int)

  case class SetFlagCommand(x: Int, y: Int, flag: FlagAction)

  sealed trait FlagAction extends EnumEntry

  object FlagType extends Enum[FlagAction] {

    val values: immutable.IndexedSeq[FlagAction] = findValues

    case object SetQuestion extends FlagAction

    case object SetRed extends FlagAction

    case object Clean extends FlagAction

  }

}


case class Game(id: Long,
                playerId: Long,
                state: GameState = GameState.Running,
                startTime: ZonedDateTime,
                finishTime: Option[ZonedDateTime] = None,
                height: Int,
                width: Int,
                mines: Int,
                cells: scala.collection.Seq[Cell],
                createdAt: ZonedDateTime,
                deletedAt: Option[ZonedDateTime] = None) {
  lazy val cellByPosition: Cells = {
    cells.foldLeft(Map.empty[Position, Cell])((map, cell) => map + (Position(cell) -> cell))
  }
}

sealed trait GameState extends EnumEntry

object GameState extends Enum[GameState] {
  val values: immutable.IndexedSeq[GameState] = findValues

  case object Created extends GameState

  case object Running extends GameState

  case object Paused extends GameState

  case object Won extends GameState

  case object Lost extends GameState

}

case class Position(x: Int, y: Int)

object Position {
  def apply(cell: Cell): Position = Position(cell.x, cell.y)
}

case class CellCreationCommand(gameId: Long, x: Int, y: Int, hasMine: Boolean, hasFlag: Boolean)

case class Cell(id: Long, gameId: Long, x: Int, y: Int, state: CellState, hasMine: Boolean, hasFlag: Boolean)

sealed trait CellState extends EnumEntry

object CellState extends Enum[CellState] {

  val values: immutable.IndexedSeq[CellState] = findValues

  case object Covered extends CellState

  case object Uncovered extends CellState

  case object QuestionFlag extends CellState

  case object RedFlag extends CellState

}
