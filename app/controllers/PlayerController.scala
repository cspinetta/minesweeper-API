package controllers

import com.github.tototoshi.play2.json4s.Json4s
import controllers.response.{BadRequestResponse, ErrorCode, InternalServerErrorResponse, NotFoundResponse, PlayerResponse}
import javax.inject._
import models.{NotUniqueError, PlayerCreationCommand, ResourceNotFound}
import org.json4s.JValue
import play.api.Logging
import play.api.mvc._
import services.PlayerService
import support.db.TxSupport

import scala.util.control.Exception.catching

/**
 * This controller handles the HTTP endpoints for managing the game users.
 */
@Singleton
class PlayerController @Inject()(val controllerComponents: ControllerComponents,
                                 val playerService: PlayerService,
                                 val json4s: Json4s)
  extends ApiController with Logging with TxSupport {

  /**
   * Creates a new player based on the request body.
   *
   * @return 200 OK - new player, otherwise 4XX or 5XX errors.
   */
  def create(): Action[JValue] = Action(json4s.json) { request =>
    catching(classOf[Exception]).either(request.body.extract[PlayerCreationCommand]) match {
      case Right(cmd) =>
        withinTx(session => playerService.create(cmd)(session)) match {
          case Right(player) =>
            logger.info(s"player successfully saved [id: ${player.id}]")
            Ok(PlayerResponse(player).asJson)
          case Left(err: NotUniqueError) =>
            logger.error(s"error while saving player. Reason: ${err.reason}", err)
            Conflict(BadRequestResponse("player already exists", ErrorCode.AlreadyExists).asJson)
          case Left(err) =>
            logger.error(s"error while saving player. Reason: ${err.reason}", err)
            InternalServerError(InternalServerErrorResponse("player cannot be saved", ErrorCode.InternalError).asJson)
        }
      case Left(err) =>
        logger.error("player cannot be parsed", err)
        BadRequest(BadRequestResponse("player cannot be parsed", ErrorCode.ValidationError).asJson)
    }
  }

  /**
   * Get player information given the id.
   *
   * @param id player id
   * @return 200 OK - the player if it's found, otherwise 4XX or 5XX errors.
   */
  def findById(id: Long): Action[AnyContent] = Action { _ =>
    withinTx(session => playerService.findById(id)(session)) match {
      case Right(player) =>
        Ok(PlayerResponse(player).asJson)
      case Left(_: ResourceNotFound) =>
        NotFound(NotFoundResponse("Player cannot be found", ErrorCode.NotFound).asJson)
      case Left(err) =>
        logger.error(s"Error while finding player. Reason: ${err.reason}", err)
        InternalServerError(InternalServerErrorResponse("Player cannot be found", ErrorCode.InternalError).asJson)
    }
  }

  /**
   * Deletes a player given the id.
   *
   * @return 204 NO_CONTENT - the player is deleted, otherwise 4XX or 5XX errors.
   */
  def delete(id: Long): Action[AnyContent] = Action { _ =>
    withinTx(session => playerService.deactivate(id)(session)) match {
      case Right(player) =>
        logger.info(s"Player successfully deactivated [id: $id]")
        NoContent
      case Left(err) =>
        logger.error(s"Error while deactivating player [id: $id]. Reason: ${err.reason}", err)
        InternalServerError(InternalServerErrorResponse(s"Player cannot be deactivated [id: $id]", ErrorCode.InternalError).asJson)
    }
  }
}
