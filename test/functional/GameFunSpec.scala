package functional

import controllers.response.{BadRequestResponse, ErrorCode, GameResponse}
import models.{CellState, GameState}
import org.json4s.jackson.JsonMethods
import org.scalatest.{EitherValues, MustMatchers, OptionValues, Outcome, fixture}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AsciiSymbols
import support.json.JsonSupport
import utils.auth.AuthUtilsT

/**
 * Functional tests for Player endpoints.
 */
class GameFunSpec extends fixture.FlatSpec with MustMatchers with EitherValues with OptionValues
  with GuiceOneAppPerSuite with JsonSupport with AuthUtilsT {

  type FixtureParam = UserTest

  private val user = "user-game-func"
  private val password = "123456789"
  private val authToken = basicAuthToken(user, password)

  private val gameMines = 10

  override def withFixture(test: OneArgTest): Outcome = {
    val reqPost = FakeRequest(POST, "/player")
      .withJsonBody(Json.parse(
        s"""
           |{
           |  "username": "$user",
           |  "password": "$password"
           |}
          """.stripMargin))
    val result = route(app, reqPost).get
    status(result) must (equal(Status.OK) or equal(Status.CONFLICT))
    withFixture(test.toNoArgTest(UserTest(authToken)))
  }

  "Game Controller" should "create a new game successfully" in { user =>
    val req = FakeRequest(POST, "/games")
      .withHeaders("Authorization" -> s"Basic ${user.userToken}")
      .withJsonBody(Json.parse(
        s"""
           |{
           |	"height": 10,
           |	"width": 10,
           |	"mines": $gameMines
           |}
            """.stripMargin))
    val eventualResult = route(app, req).get

    status(eventualResult) mustBe Status.OK
    contentType(eventualResult) mustBe Some("application/json")
    val gameResponse = JsonMethods.parse(contentAsString(eventualResult)).extract[GameResponse]
    gameResponse.cells.size must be(0)
    gameResponse.state must be(GameState.Created)
    gameResponse.finishTime mustBe None
  }

  it should "draw the board before the game started" in { user =>
    val req = FakeRequest(GET, "/games/1/ascii")
      .withHeaders("Authorization" -> s"Basic ${user.userToken}")
    val eventualResult = route(app, req).get

    status(eventualResult) mustBe Status.OK
    contentType(eventualResult) mustBe Some("text/plain")
    contentAsString(eventualResult).length must be > (0)
  }

  it should "reveal the first cell" in { user =>
    val req = FakeRequest(PATCH, "/games/1")
      .withHeaders("Authorization" -> s"Basic ${user.userToken}")
      .withJsonBody(Json.parse(
        s"""
           |{
           |	"action": "reveal",
           |	"position": {
           |		"x": 8,
           |		"y": 9
           |	}
           |}
            """.stripMargin))
    val eventualResult = route(app, req).get

    status(eventualResult) mustBe Status.OK
    contentType(eventualResult) mustBe Some("application/json")
    val gameResponse = JsonMethods.parse(contentAsString(eventualResult)).extract[GameResponse]
    gameResponse.cells.size must be(10 * 10)
    gameResponse.cells.count(_.state == CellState.Uncovered) must be >= (1)
    gameResponse.state must be(GameState.Running)
    gameResponse.finishTime mustBe None
  }

  it should "draw the board after the game started" in { user =>
    val req = FakeRequest(GET, "/games/1/ascii")
      .withHeaders("Authorization" -> s"Basic ${user.userToken}")
    val eventualResult = route(app, req).get

    status(eventualResult) mustBe Status.OK
    contentType(eventualResult) mustBe Some("text/plain")
    contentAsString(eventualResult).length must be > (0)
  }

  it should "draw the board showing the mines" in { user =>
    val req = FakeRequest(GET, "/games/1/ascii?debug=true")
      .withHeaders("Authorization" -> s"Basic ${user.userToken}")
    val eventualResult = route(app, req).get

    status(eventualResult) mustBe Status.OK
    contentType(eventualResult) mustBe Some("text/plain")
    val body = contentAsString(eventualResult)
    body.length must be > (0)
    AsciiSymbols.mine.r.findAllIn(body).length must be(gameMines)
  }

  it should "be able to pause when it's running" in { user =>
    val req = FakeRequest(POST, "/games/1/state")
      .withHeaders("Authorization" -> s"Basic ${user.userToken}")
      .withJsonBody(Json.parse(
        s"""
           |{
           |	"action": "pause"
           |}
            """.stripMargin))
    val eventualResult = route(app, req).get

    status(eventualResult) mustBe Status.OK
    contentType(eventualResult) mustBe Some("application/json")
    val gameResponse = JsonMethods.parse(contentAsString(eventualResult)).extract[GameResponse]
    gameResponse.state must be(GameState.Paused)
  }

  it should "be able to resume when it's paused" in { user =>
    val req = FakeRequest(POST, "/games/1/state")
      .withHeaders("Authorization" -> s"Basic ${user.userToken}")
      .withJsonBody(Json.parse(
        s"""
           |{
           |	"action": "resume"
           |}
            """.stripMargin))
    val eventualResult = route(app, req).get

    status(eventualResult) mustBe Status.OK
    contentType(eventualResult) mustBe Some("application/json")
    val gameResponse = JsonMethods.parse(contentAsString(eventualResult)).extract[GameResponse]
    gameResponse.state must be(GameState.Running)
  }

  it should "lose when reveals a mine" in { user =>
    val req = FakeRequest(GET, "/games/1")
      .withHeaders("Authorization" -> s"Basic ${user.userToken}")
    val eventualResult = route(app, req).get

    status(eventualResult) mustBe Status.OK
    contentType(eventualResult) mustBe Some("application/json")
    val gameResponse = JsonMethods.parse(contentAsString(eventualResult)).extract[GameResponse]

    val cell = gameResponse.cells.find(_.hasMine).value
    val req2 = FakeRequest(PATCH, "/games/1")
      .withHeaders("Authorization" -> s"Basic ${user.userToken}")
      .withJsonBody(Json.parse(
        s"""
           |{
           |	"action": "reveal",
           |	"position": {
           |		"x": ${cell.x},
           |		"y": ${cell.y}
           |	}
           |}
            """.stripMargin))
    val eventualResult2 = route(app, req2).get

    status(eventualResult2) mustBe Status.OK
    contentType(eventualResult2) mustBe Some("application/json")
    val gameResponse2 = JsonMethods.parse(contentAsString(eventualResult2)).extract[GameResponse]
    gameResponse2.state must be(GameState.Lost)
  }

  it should "not be able to pause when it's finished" in { user =>
    val req = FakeRequest(POST, "/games/1/state")
      .withHeaders("Authorization" -> s"Basic ${user.userToken}")
      .withJsonBody(Json.parse(
        s"""
           |{
           |	"action": "pause"
           |}
            """.stripMargin))
    val eventualResult = route(app, req).get

    status(eventualResult) mustBe Status.BAD_REQUEST
    contentType(eventualResult) mustBe Some("application/json")
    val errorResponse = JsonMethods.parse(contentAsString(eventualResult)).extract[BadRequestResponse]
    errorResponse.errorCode must be(ErrorCode.ClientError)
  }

  it should "not be able to resume when it's finished" in { user =>
    val req = FakeRequest(POST, "/games/1/state")
      .withHeaders("Authorization" -> s"Basic ${user.userToken}")
      .withJsonBody(Json.parse(
        s"""
           |{
           |	"action": "resume"
           |}
            """.stripMargin))
    val eventualResult = route(app, req).get

    status(eventualResult) mustBe Status.BAD_REQUEST
    contentType(eventualResult) mustBe Some("application/json")
    val errorResponse = JsonMethods.parse(contentAsString(eventualResult)).extract[BadRequestResponse]
    errorResponse.errorCode must be(ErrorCode.ClientError)
  }
}

case class UserTest(userToken: String)
