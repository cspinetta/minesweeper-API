package controllers

import com.github.tototoshi.play2.json4s.Json4s
import controllers.response._
import javax.inject._
import models.GameActions._
import org.json4s.JValue
import play.api.Logging
import play.api.mvc._
import services.{AsciiPrinterService, GameService}
import support.auth.UserAuthenticatedBuilder
import support.db.TxSupport

import scala.util.control.Exception.catching

/**
 * This controller handles the HTTP endpoints for managing the games.
 */
@Singleton
class GameController @Inject()(val controllerComponents: ControllerComponents,
                               val gameService: GameService,
                               val asciiPrinterService: AsciiPrinterService,
                               val json4s: Json4s,
                               val userAuthenticated: UserAuthenticatedBuilder)
  extends ApiController with Logging with TxSupport {

  /**
   * Create a new game based on the request body.
   *
   * @return 200 OK - new game created, otherwise 4XX or 5XX errors.
   */
  def create(): Action[JValue] = userAuthenticated(json4s.json) { req =>
    catching(classOf[Exception]).either(req.body.extract[GameCreationCommand]) match {
      case Right(cmd) =>
        handleAppError(withinTx(session => gameService.create(req.user.userId, cmd)(session)),
          operation = "create a game", entity = "game") {
          game =>
            logger.info(s"game successfully created [id: ${game.id}, player_id: ${game.playerId}, " +
              s"height: ${game.height}, width: ${game.width}, mines: ${game.mines}]")
            Ok(GameResponse(game).asJson)
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
  def findById(id: Long): Action[AnyContent] = userAuthenticated { req =>
    handleAppError(
      withinTx(session => gameService.validateGameByUser(id, req.user.userId)(session)),
      operation = "find a game", entity = "game") {
      game =>
        Ok(GameResponse(game).asJson)
    }

  }

  /**
   * Update cell state given the game id, the cell position and the desired new state.
   *
   * @return 200 OK - the flag is added, otherwise 4XX or 5XX errors.
   */
  def setCellState(id: Long): Action[JValue] = userAuthenticated(json4s.json) { req =>
    catching(classOf[Exception]).either(req.body.extract[SetCellStateCommand]) match {
      case Right(cmd) =>
        handleAppError(
          withinTx(session =>
            gameService.validateGameByUser(id, req.user.userId)(session).flatMap(game =>
              gameService.updateCellState(game, cmd)(session))),
          operation = "update cell state (reveal / set a flag / clean)", "cell") { game =>
          logger.debug(s"game successfully updated [id: ${game.id}, player_id: ${game.playerId}, " +
            s"SetCellCommand: $cmd]")
          Ok(GameResponse(game).asJson)
        }
      case Left(err) =>
        logger.error("set cell request cannot be parsed", err)
        BadRequest(BadRequestResponse("set cell request cannot be parsed", ErrorCode.ValidationError).asJson)
    }
  }

  /**
   * Update game state (pause / resume) given the game id and the desired new state.
   *
   * @return 200 OK - the game is updated, otherwise 4XX or 5XX errors.
   */
  def updateGameState(id: Long): Action[JValue] = userAuthenticated(json4s.json) { req =>
    catching(classOf[Exception]).either(req.body.extract[GameStateCommand]) match {
      case Right(cmd) =>
        handleAppError(
          withinTx(session =>
            gameService.validateGameByUser(id, req.user.userId)(session).flatMap(game =>
              gameService.updateGameState(game, cmd)(session))),
          operation = "pause / resume a game", entity = "game") { game =>
          logger.debug(s"game successfully updated [id: ${game.id}, player_id: ${game.playerId}, " +
            s"UpdateGameStateCommand: $cmd]")
          Ok(GameResponse(game).asJson)
        }
      case Left(err) =>
        logger.error("update game state request cannot be parsed", err)
        BadRequest(BadRequestResponse("update game state request cannot be parsed", ErrorCode.ValidationError).asJson)
    }
  }

  /**
   * Get ASCII representation of the game board given the id.
   *
   * @param id game id
   * @return 200 OK - the game if it's found, otherwise 4XX or 5XX errors.
   */
  def boardInASCII(id: Long, debug: Option[Boolean]): Action[AnyContent] = userAuthenticated { req =>
    handleAppError(
      withinTx(session =>
        gameService.validateGameByUser(id, req.user.userId)(session).map(game =>
          asciiPrinterService.getBoardInAscii(game, debug.getOrElse(false)))),
      operation = "draw the board in ASCII", entity = "game") { ascii =>
      Ok(ascii)
    }
  }
}
