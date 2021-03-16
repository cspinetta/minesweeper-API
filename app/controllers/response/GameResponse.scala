package controllers.response

import java.time.ZonedDateTime

import models.{CellState, Game, GameState}

case class GameResponse(id: Long,
                        state: GameState,
                        startTime: ZonedDateTime,
                        finishTime: Option[ZonedDateTime] = None,
                        cells: scala.collection.Seq[CellResponse])

case class CellResponse(x: Int, y: Int, state: CellState, hasMine: Boolean, adjacentMines: Int)

object GameResponse {
  def apply(game: Game): GameResponse = {
    val cells = game.cells.map(c => CellResponse(
      x = c.x,
      y = c.y,
      state = c.state,
      hasMine = c.hasMine,
      adjacentMines = c.adjacentMines,
    ))
    new GameResponse(
      id = game.id,
      state = game.state,
      startTime = game.startTime,
      finishTime = game.finishTime,
      cells = cells)
  }
}
