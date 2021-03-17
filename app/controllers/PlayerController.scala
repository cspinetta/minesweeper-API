package controllers

import com.github.tototoshi.play2.json4s.Json4s
import controllers.response._
import javax.inject._
import models.PlayerCreationCommand
import org.json4s.JValue
import play.api.Logging
import play.api.mvc._
import services.PlayerService
import support.auth.UserAuthenticatedBuilder
import support.db.TxSupport

import scala.util.control.Exception.catching

/**
 * This controller handles the HTTP endpoints for managing the game users.
 */
@Singleton
class PlayerController @Inject()(val controllerComponents: ControllerComponents,
                                 val playerService: PlayerService,
                                 val json4s: Json4s,
                                 val userAuthenticated: UserAuthenticatedBuilder)
  extends ApiController with Logging with TxSupport {

  /**
   * Creates a new player based on the request body.
   *
   * @return 200 OK - new player, otherwise 4XX or 5XX errors.
   */
  def create(): Action[JValue] = Action(json4s.json) { req =>
    catching(classOf[Exception]).either(req.body.extract[PlayerCreationCommand]) match {
      case Right(cmd) =>
        handleAppError(
          withinTx(session => playerService.create(cmd)(session)),
          operation = "create a user", entity = "user") { user =>
          logger.info(s"user successfully saved [id: ${user.id}]")
          Ok(PlayerResponse(user).asJson)
        }
      case Left(err) =>
        logger.error("user cannot be parsed", err)
        BadRequest(BadRequestResponse("user cannot be parsed", ErrorCode.ValidationError).asJson)
    }
  }

  /**
   * Get player information.
   *
   * @return 200 OK - the player if it's found, otherwise 4XX or 5XX errors.
   */
  def details(): Action[AnyContent] = userAuthenticated { req =>
    handleAppError(
      withinTx(session => playerService.findById(req.user.userId)(session)),
      operation = "get user details", entity = "user") { player =>
      Ok(PlayerResponse(player).asJson)
    }
  }

  /**
   * Deletes a player.
   *
   * @return 204 NO_CONTENT - the player is deleted, otherwise 4XX or 5XX errors.
   */
  def delete(): Action[AnyContent] = userAuthenticated { req =>
    handleAppError(
      withinTx(session => playerService.deactivate(req.user.userId)(session)),
      operation = "delete a user", entity = "user") { _ =>
      logger.info(s"Player successfully deactivated [id: ${req.user.userId}]")
      NoContent
    }
  }
}
