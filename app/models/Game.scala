package models

import java.time.ZonedDateTime

import enumeratum._

import scala.collection.immutable

case class Game(id: Long, playerId: Long, state: GameState = GameState.Running, startTime: ZonedDateTime,
                finishTime: Option[ZonedDateTime] = None, board: Board)


sealed trait GameState extends EnumEntry

object GameState extends Enum[GameState] {
  val values: immutable.IndexedSeq[GameState] = findValues

  case object Running extends GameState

  case object Won extends GameState

  case object Lost extends GameState

}
