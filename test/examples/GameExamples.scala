package examples

import java.time.ZonedDateTime

import models.GameActions.GameCreationCommand
import models.{Cell, CellState, Game, GameState, Player, PlayerCreationCommand}
import org.json4s.JValue
import support.json.JsonSupport

object GameExamples extends JsonSupport {

  val now: ZonedDateTime = ZonedDateTime.now()

  object GameOne {

    val CreationCommand: GameCreationCommand = GameCreationCommand(playerId = 1L, height = 10, width = 10, mines = 4)

    val Sample: Game = Game(
      id = 1L,
      playerId = CreationCommand.playerId,
      state = GameState.Running,
      startTime = now,
      finishTime = None,
      height = CreationCommand.height,
      width = CreationCommand.width,
      mines = CreationCommand.mines,
      cells = Seq(),
      createdAt = now,
      deletedAt = None)

    val Json: JValue = Sample.asJson
  }
}
