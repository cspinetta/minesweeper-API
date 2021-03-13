import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.json.JsonSupport

/**
 * Functional tests start a Play application internally, available
 * as `app`.
 */
class FunctionalSpec extends PlaySpec with GuiceOneAppPerSuite with JsonSupport {

  "Routes" should {
    "send 404 on a bad request" in {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }

    "send 200 on a good request" in {
      route(app, FakeRequest(GET, "/health-check")).map(status(_)) mustBe Some(OK)
    }
  }

  "HealthCheckController" should {
    "check app health" in {
      val healthCheckResponse = route(app, FakeRequest(GET, "/health-check")).get

      status(healthCheckResponse) mustBe Status.OK
      contentType(healthCheckResponse) mustBe Some("application/json")
      contentAsString(healthCheckResponse) must include("OK")
    }
  }

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
    }

    "get last player" in {
      val req = FakeRequest(GET, "/player/1")
      val eventualResult = route(app, req).get

      status(eventualResult) mustBe Status.OK
      contentType(eventualResult) mustBe Some("application/json")
      contentAsString(eventualResult) must include(playerName)
    }

    "delete player" in {
      val req = FakeRequest(DELETE, "/player/1")
      val eventualResult = route(app, req).get

      status(eventualResult) mustBe Status.NO_CONTENT
    }
  }
}
