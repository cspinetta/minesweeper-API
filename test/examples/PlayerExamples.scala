package examples

import java.time.ZonedDateTime

import models.Player
import org.json4s.JValue
import support.json.JsonSupport

object PlayerExamples extends JsonSupport {

  val Player_One_Sample: Player = Player(
    id = 1L,
    username = "player_one",
    createdAt = ZonedDateTime.now(),
  )

  val Player_One_Json: JValue = Player_One_Sample.asJson
}
