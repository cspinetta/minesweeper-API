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
 * Functional tests for Health Checks endpoints.
 */
class HealthCheckFunSpec extends PlaySpec with GuiceOneAppPerSuite with JsonSupport {

  "HealthCheckController" should {
    "check app health" in {
      val healthCheckResponse = route(app, FakeRequest(GET, "/health-check")).get

      status(healthCheckResponse) mustBe Status.OK
      contentType(healthCheckResponse) mustBe Some("application/json")
      contentAsString(healthCheckResponse) must include("OK")
    }
  }
}
