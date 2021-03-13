package models

import enumeratum._

import scala.collection.immutable

case class Board(cells: Map[Position, Cell])

case class Position(x: Int, y: Int)

case class Cell(hasFlag: Boolean, state: CellState, hasMine: Boolean)

sealed trait CellState extends EnumEntry

object CellState extends Enum[CellState] {

  val values: immutable.IndexedSeq[CellState] = findValues

  case object Covered extends CellState

  case object Uncovered extends CellState

  case object Flagged extends CellState

}
