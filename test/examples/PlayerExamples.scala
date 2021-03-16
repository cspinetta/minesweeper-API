package examples

import java.time.ZonedDateTime

import models.{Player, PlayerCreationCommand}
import org.json4s.JValue
import support.auth.AuthUtils
import support.json.JsonSupport

object PlayerExamples extends JsonSupport {

  val now: ZonedDateTime = ZonedDateTime.now()

  object PlayerOne {

    val CreationCommand: PlayerCreationCommand = PlayerCreationCommand(username = "player_one", password = "123456789")

    val Sample: Player = Player(
      id = 1L,
      username = CreationCommand.username,
      encodedPass = AuthUtils.hash(CreationCommand.password),
      createdAt = now,
    )

    val Json: JValue = Sample.asJson
  }

}
