package controllers

import com.github.tototoshi.play2.json4s.Json4s
import controllers.response._
import javax.inject._
import models.GameActions._
import models.{InvalidParametersError, ResourceNotFound}
import org.json4s.JValue
import play.api.Logging
import play.api.mvc._
import services.{AsciiPrinterService, GameService}
import support.db.TxSupport

import scala.util.control.Exception.catching

/**
 * This controller handles the HTTP endpoints for managing the game users.
 */
@Singleton
class GameController @Inject()(val controllerComponents: ControllerComponents,
                               val gameService: GameService,
                               val asciiPrinterService: AsciiPrinterService,
                               val json4s: Json4s)
  extends ApiController with Logging with TxSupport {

  /**
   * Create a new game based on the request body.
   *
   * @return 200 OK - new game created, otherwise 4XX or 5XX errors.
   */
  def create(): Action[JValue] = Action(json4s.json) { request =>
    catching(classOf[Exception]).either(request.body.extract[GameCreationCommand]) match {
      case Right(cmd) =>
        withinTx(session => gameService.create(cmd)(session)) match {
          case Right(game) =>
            logger.info(s"game successfully created [id: ${game.id}, player_id: ${game.playerId}, " +
              s"height: ${game.height}, width: ${game.width}, mines: ${game.mines}]")
            Ok(GameResponse(game).asJson)
          case Left(err: InvalidParametersError) =>
            logger.error(s"error while creating a new game. Reason: ${err.reason}", err)
            UnprocessableEntity(UnprocessableResponse("game cannot be created", ErrorCode.ValidationError).asJson)
          case Left(err) =>
            logger.error(s"error while creating a new game. Reason: ${err.reason}", err)
            InternalServerError(InternalServerErrorResponse("game cannot be created", ErrorCode.InternalError).asJson)
        }
      case Left(err) =>
        logger.error("new game request cannot be parsed", err)
        BadRequest(BadRequestResponse("new game request cannot be parsed", ErrorCode.ValidationError).asJson)
    }
  }

  /**
   * Get game information given the id.
   *
   * @param id game id
   * @return 200 OK - the game if it's found, otherwise 4XX or 5XX errors.
   */
  def findById(id: Long): Action[AnyContent] = Action { _ =>
    withinTx(session => gameService.findById(id)(session)) match {
      case Right(game) =>
        Ok(GameResponse(game).asJson)
      case Left(_: ResourceNotFound) =>
        NotFound(NotFoundResponse("Game cannot be found", ErrorCode.NotFound).asJson)
      case Left(err) =>
        logger.error(s"Error while finding game. Reason: ${err.reason}", err)
        InternalServerError(InternalServerErrorResponse("Game cannot be found", ErrorCode.InternalError).asJson)
    }
  }

  /**
   * Update cell state given the game id, the cell position and the desired new state.
   *
   * @return 200 OK - the flag is added, otherwise 4XX or 5XX errors.
   */
  def setCellState(id: Long): Action[JValue] = Action(json4s.json) { request =>
    catching(classOf[Exception]).either(request.body.extract[SetCellStateCommand]) match {
      case Right(cmd) =>
        withinTx(session => gameService.updateCellState(id, cmd)(session)) match {
          case Right(game) =>
            logger.debug(s"game successfully updated [id: ${game.id}, player_id: ${game.playerId}, " +
              s"SetCellCommand: $cmd]")
            Ok(GameResponse(game).asJson)
          case Left(_: ResourceNotFound) =>
            NotFound(NotFoundResponse("Game / cell position cannot be found", ErrorCode.NotFound).asJson)
          case Left(err) =>
            logger.error(s"error while updating a cell. Reason: ${err.reason}", err)
            InternalServerError(InternalServerErrorResponse("cell cannot be updated", ErrorCode.InternalError).asJson)
        }
      case Left(err) =>
        logger.error("set cell request cannot be parsed", err)
        BadRequest(BadRequestResponse("set cell request cannot be parsed", ErrorCode.ValidationError).asJson)
    }
  }

  /**
   * Get ASCII representation of the game board given the id.
   *
   * @param id game id
   * @return 200 OK - the game if it's found, otherwise 4XX or 5XX errors.
   */
  def asciiBoard(id: Long, debug: Option[Boolean]): Action[AnyContent] = Action { _ =>
    withinTx(session => asciiPrinterService.getAsciiBoard(id, debug.getOrElse(false))(session)) match {
      case Right(ascii) =>
        Ok(ascii)
      case Left(_: ResourceNotFound) =>
        NotFound(NotFoundResponse("Game cannot be found", ErrorCode.NotFound).asJson)
      case Left(err) =>
        logger.error(s"Error while generating ASCII game. Reason: ${err.reason}", err)
        InternalServerError(InternalServerErrorResponse("Game ASCII cannot be generated", ErrorCode.InternalError).asJson)
    }
  }
}
