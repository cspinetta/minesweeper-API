package functional

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.json.JsonSupport

/**
 * Functional tests for global handlers.
 */
class GlobalHandlersFunSpec extends PlaySpec with GuiceOneAppPerSuite with JsonSupport {

  "Routes" should {
    "send 404 on a bad request" in {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }

    "send 200 on a good request" in {
      route(app, FakeRequest(GET, "/health-check")).map(status(_)) mustBe Some(OK)
    }
  }
}
