package functional

import java.util.Base64

import controllers.response.ErrorCode
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

    "create a player" in {

      val playerName = "player_1"
      val password = "123456789"

      val req = FakeRequest(POST, "/player")
        .withJsonBody(Json.parse(
          s"""
             |{
             |  "username": "$playerName",
             |  "password": "$password"
             |}
          """.stripMargin))
      val eventualResult = route(app, req).get

      status(eventualResult) mustBe Status.OK
      contentType(eventualResult) mustBe Some("application/json")
      contentAsString(eventualResult) must include("player_1")
      println("CONTENT" + contentAsString(eventualResult))
    }

    "get the player details" in {
      val playerName = "player_2"
      val password = "123456789"
      val passToken = basicAuthToken(playerName, password)

      val reqPost = FakeRequest(POST, "/player")
        .withJsonBody(Json.parse(
          s"""
             |{
             |  "username": "$playerName",
             |  "password": "$password"
             |}
          """.stripMargin))
      route(app, reqPost).get

      val req = FakeRequest(GET, s"/player")
        .withHeaders("Authorization" -> s"Basic $passToken")
      val result = route(app, req).get

      status(result) mustBe Status.OK
      contentType(result) mustBe Some("application/json")
      contentAsString(result) must include(playerName)
    }

    "respond with Unauthorized when trying to get the user details with bad credentials" in {
      val req = FakeRequest(GET, "/player")
        .withHeaders("Authorization" -> s"Basic ${Base64.getEncoder.encodeToString("player_1:987654321".getBytes)}")
      val eventualResult = route(app, req).get

      status(eventualResult) mustBe Status.UNAUTHORIZED
      contentType(eventualResult) mustBe Some("application/json")
      contentAsString(eventualResult) must include(ErrorCode.Unauthorized)
    }

    "delete player" in {
      val playerName = "player_3"
      val password = "123456789"
      val passToken = basicAuthToken(playerName, password)

      val reqPost = FakeRequest(POST, "/player")
        .withJsonBody(Json.parse(
          s"""
             |{
             |  "username": "$playerName",
             |  "password": "$password"
             |}
          """.stripMargin))
      route(app, reqPost).get

      val req = FakeRequest(DELETE, "/player")
        .withHeaders("Authorization" -> s"Basic $passToken")
      val eventualResult = route(app, req).get

      status(eventualResult) mustBe Status.NO_CONTENT
    }
  }

  private def basicAuthToken(user: String, pass: String): String = {
    Base64.getEncoder.encodeToString(s"$user:$pass".getBytes)
  }
}
