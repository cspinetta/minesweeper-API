package examples

import java.time.ZonedDateTime

import models.GameActions.GameCreationCommand
import models.{Game, GameState}
import org.json4s.JValue
import support.json.JsonSupport

object GameExamples extends JsonSupport {

  val now: ZonedDateTime = ZonedDateTime.now()

  object GameOne {

    val CreationCommand: GameCreationCommand = GameCreationCommand(height = 10, width = 10, mines = 4)

    val Sample: Game = Game(
      id = 1L,
      playerId = 1L,
      state = GameState.Running,
      startTime = now,
      finishTime = None,
      lastStartToPlay = now,
      totalTimeSeconds = 0,
      height = CreationCommand.height,
      width = CreationCommand.width,
      mines = CreationCommand.mines,
      cells = Seq(),
      createdAt = now,
      deletedAt = None)

    val Json: JValue = Sample.asJson
  }

}
