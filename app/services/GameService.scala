package services

import javax.inject.{Inject, Singleton}
import models.GameActions._
import models._
import play.api.Logging
import repositories.{CellRepository, GameRepository}
import scalikejdbc.DBSession

@Singleton
class GameService @Inject()(val playerService: PlayerService, val gameRepository: GameRepository, val cellRepository: CellRepository) extends Logging {

  def create(cmd: GameCreationCommand)(implicit session: DBSession): Either[AppError, Game] = {
    for {
      _ <- validatePlayerExists(cmd)
      game <- gameRepository.create(cmd, GameState.Created)
//      _ <- cellRepository.create(generateCells(cmd, game.id))
//      game <- gameRepository.find(game.id)
    } yield game
  }

  def findById(id: Long)(implicit session: DBSession): Either[AppError, Game] = {
    gameRepository.find(id)
  }

  def revealCell(id: Long, cmd: RevealCellCommand)(implicit session: DBSession): Either[AppError, Game] = {
    // generate cells at the first action
    ???
  }

  def setFlag(id: Long, cmd: SetFlagCommand)(implicit session: DBSession): Either[AppError, Game] = {
    ???
  }

  private def validatePlayerExists(cmd: GameCreationCommand)(implicit session: DBSession): Either[AppError, Unit] = {
    playerService.exists(cmd.playerId)
      .flatMap {
        case true =>
          println("...")
          Right(())
        case false =>
          println("------...")
          Left(InvalidParametersError("Player not exists"))
      }
  }

  private def generateCells(cmd: GameCreationCommand, gameId: Long): Seq[CellCreationCommand] = {
    for {
      x <- (0 to cmd.width)
      y <- (0 to cmd.height)
    } yield CellCreationCommand(
      gameId = gameId,
      x = x,
      y = y,
      hasMine = false, // FIXME
      hasFlag = false,
    )
  }
}
