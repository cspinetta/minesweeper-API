package services

import com.typesafe.config.ConfigFactory
import conf.AppConfigProvider
import examples.{GameExamples, PlayerExamples}
import models.{CellState, GameState, Position}
import org.scalatest.{EitherValues, MustMatchers, fixture}
import play.api.Configuration
import repositories.{CellRepository, GameRepository, PlayerRepository}
import utils.db.{AutoRollback, Connection}

class GameServiceSpec extends fixture.FlatSpec with Connection with AutoRollback with MustMatchers with EitherValues {

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
    val game = gameService.revealCell(initialGame, position).right.value

    game.id must be(initialGame.id)
    game.width must be(cmd.width)
    game.height must be(cmd.height)
    game.mines must be(cmd.mines)
    game.state must be(GameState.Running)
    game.finishTime must be(None)
    game.deletedAt must be(None)

    game.cells.size must be(cmd.width * cmd.height)
    game.cells.count(_.hasMine) must be(cmd.mines)
    game.cellByPosition(position).state must be(CellState.Uncovered)
  }
}
