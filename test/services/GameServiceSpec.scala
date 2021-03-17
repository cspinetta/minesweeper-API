package services

import com.typesafe.config.ConfigFactory
import conf.AppConfigProvider
import examples.{GameExamples, PlayerExamples}
import models.GameActions.{CellAction, SetCellStateCommand}
import models.{CellState, Game, GameState, Position}
import org.scalatest.{EitherValues, MustMatchers, OptionValues, fixture}
import play.api.Configuration
import repositories.{CellRepository, GameRepository, PlayerRepository}
import utils.db.{AutoRollback, Connection}

import scala.annotation.tailrec

class GameServiceSpec extends fixture.FlatSpec with Connection with AutoRollback with MustMatchers
  with EitherValues with OptionValues {

  lazy val config: Configuration = Configuration(ConfigFactory.parseString(
    s"""
      """.stripMargin)
    .withFallback(ConfigFactory.load()))

  lazy val playerRepository = new PlayerRepository
  lazy val playerService = new PlayerService(playerRepository)

  lazy val configProvider = new AppConfigProvider(config)

  lazy val gameRepository = new GameRepository
  lazy val cellRepository = new CellRepository
  lazy val gameService = new GameService(configProvider, playerService, gameRepository, cellRepository)

  behavior of "Game service"

  it should "create a new game successfully" in { implicit session =>
    playerService.create(PlayerExamples.PlayerOne.CreationCommand.copy(username = "game-service-test-1")).right.value.id
    val cmd = GameExamples.GameOne.CreationCommand
    val result = gameService.create(GameExamples.GameOne.Sample.playerId, cmd)
    val game = result.right.value
    game.id mustBe >(0L)
    game.width must be(cmd.width)
    game.height must be(cmd.height)
    game.cells must be(Seq())
    game.mines must be(cmd.mines)
    game.state must be(GameState.Created)
    game.finishTime must be(None)
    game.deletedAt must be(None)
  }

  it should "initialize the game at the first reveal" in { implicit session =>
    val playerId = playerService.create(PlayerExamples.PlayerOne.CreationCommand.copy(username = "game-service-test-2")).right.value.id
    val cmd = GameExamples.GameOne.CreationCommand
    val result = gameService.create(playerId, cmd)
    val initialGame = result.right.value

    val position = Position(x = 2, y = 2)
    val game = gameService.updateCellState(initialGame, SetCellStateCommand(action = CellAction.Reveal, position = position)).right.value

    game.id must be(initialGame.id)
    game.width must be(cmd.width)
    game.height must be(cmd.height)
    game.mines must be(cmd.mines)
    game.state must (equal(GameState.Running) or equal(GameState.Won))
    game.finishTime must be(None)
    game.deletedAt must be(None)

    game.cells.size must be(cmd.width * cmd.height)
    game.cells.count(_.hasMine) must be(cmd.mines)
    game.cellByPosition(position).state must be(CellState.Uncovered)
  }

  it should "win the game when reveals all cells without mine" in { implicit session =>
    val playerId = playerService.create(PlayerExamples.PlayerOne.CreationCommand.copy(username = "game-service-test-3")).right.value.id
    val cmd = GameExamples.GameOne.CreationCommand
    val result = gameService.create(playerId, cmd)
    val initialGame = result.right.value

    val position = Position(x = 2, y = 2)
    val game = gameService.revealCell(initialGame, position).right.value

    val totalCells = game.cells.size

    @tailrec
    def revealAll(game: Game, revealCount: Int): Unit = {
      revealCount must be < totalCells
      game.cells.find(c => !c.hasMine && c.state != CellState.Uncovered) match {
        case Some(cell) =>
          val updatedGame = gameService.revealCell(game, cell.position).right.value
          revealAll(updatedGame, revealCount + 1)
        case None => ()
      }
    }

    revealAll(game, revealCount = 0)

    val finishedGame = gameService.findById(game.id).right.value

    finishedGame.state must be(GameState.Won)
    finishedGame.finishTime.isDefined must be(true)
    finishedGame.deletedAt.isDefined must be(false)
  }

  it should "lose the game when reveals a cell with mine" in { implicit session =>
    val playerId = playerService.create(PlayerExamples.PlayerOne.CreationCommand.copy(username = "game-service-test-4")).right.value.id
    val cmd = GameExamples.GameOne.CreationCommand
    val result = gameService.create(playerId, cmd)
    val initialGame = result.right.value

    val position = Position(x = 2, y = 2)
    val game = gameService.revealCell(initialGame, position).right.value

    val cell = game.cells.find(c => c.hasMine && c.state != CellState.Uncovered).value

    gameService.revealCell(game, cell.position).right.value

    val finishedGame = gameService.findById(game.id).right.value

    finishedGame.state must be(GameState.Lost)
    finishedGame.finishTime.isDefined must be(true)
    finishedGame.deletedAt.isDefined must be(false)
  }

  it should "be able to add a red flag" in { implicit session =>
    val playerId = playerService.create(PlayerExamples.PlayerOne.CreationCommand.copy(username = "game-service-test-5")).right.value.id
    val cmd = GameExamples.GameOne.CreationCommand
    val result = gameService.create(playerId, cmd)
    val initialGame = result.right.value

    val position = Position(x = 2, y = 2)
    val game = gameService.revealCell(initialGame, position).right.value

    val cell = game.cells.find(c => !c.hasMine && c.state != CellState.Uncovered).value

    val finishedGame = gameService.updateCellState(game, SetCellStateCommand(action = CellAction.SetRedFlag, position = cell.position)).right.value

    finishedGame.state must be(GameState.Running)
    finishedGame.cells.find(_.id == cell.id).value.state must be(CellState.RedFlag)
    finishedGame.finishTime must be(None)
    finishedGame.deletedAt must be(None)
  }
}
