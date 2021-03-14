package functional

import controllers.response.{ErrorCode, PlayerResponse}
import org.json4s.jackson.JsonMethods
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.json.JsonSupport

/**
 * Functional tests for Player endpoints.
 */
class PlayerFunSpec extends PlaySpec with GuiceOneAppPerSuite with JsonSupport {

  "PlayerController" should {

    val playerName = "player_1"

    "create a player" in {
      val req = FakeRequest(POST, "/player")
        .withJsonBody(Json.parse(
          s"""
             |{
             |  "username": "$playerName"
             |}
          """.stripMargin))
      val eventualResult = route(app, req).get

      status(eventualResult) mustBe Status.OK
      contentType(eventualResult) mustBe Some("application/json")
      contentAsString(eventualResult) must include("player_1")
      println("CONTENT" + contentAsString(eventualResult))
    }

    "get a player" in {
      val playerName = "player_to_test_get"
      val reqPost = FakeRequest(POST, "/player")
        .withJsonBody(Json.parse(
          s"""
             |{
             |  "username": "$playerName"
             |}
          """.stripMargin))
      val postResult = route(app, reqPost).get
      val newPlayer = JsonMethods.parse(contentAsString(postResult)).extract[PlayerResponse]

      val req = FakeRequest(GET, s"/player/${newPlayer.id}")
      val result = route(app, req).get

      status(result) mustBe Status.OK
      contentType(result) mustBe Some("application/json")
      contentAsString(result) must include(playerName)
    }

    "respond with NotFound when trying to get a nonexistent player" in {
      val req = FakeRequest(GET, "/player/909090")
      val eventualResult = route(app, req).get

      status(eventualResult) mustBe Status.NOT_FOUND
      contentType(eventualResult) mustBe Some("application/json")
      contentAsString(eventualResult) must include(ErrorCode.NotFound)
    }

    "respond with BadRequest when trying to get a player with an invalid ID" in {
      val req = FakeRequest(GET, "/player/a1b2c3")
      val eventualResult = route(app, req).get

      status(eventualResult) mustBe Status.BAD_REQUEST
      contentType(eventualResult) mustBe Some("application/json")
      contentAsString(eventualResult) must include(ErrorCode.ClientError)
    }

    "delete player" in {
      val req = FakeRequest(DELETE, "/player/1")
      val eventualResult = route(app, req).get

      status(eventualResult) mustBe Status.NO_CONTENT
    }
  }
}
