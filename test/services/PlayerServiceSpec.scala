package services

import examples.PlayerExamples
import org.scalatest.{EitherValues, MustMatchers, fixture}
import repositories.PlayerRepository
import utils.db.{AutoRollback, Connection}

class PlayerServiceSpec extends fixture.FlatSpec with Connection with AutoRollback with MustMatchers with EitherValues {

  val playerRepository = new PlayerRepository
  val playerService = new PlayerService(playerRepository)

  behavior of "Player service"

  it should "create a new player successfully" in { implicit session =>
    val cmd = PlayerExamples.PlayerOne.CreationCommand
    val result = playerService.create(cmd)
    val player = result.right.value
    player.id mustBe >(0L)
    player.username must be(cmd.username)
  }

  it should "not create a new player when exists another one with the same username" in { implicit session =>
    val cmd = PlayerExamples.PlayerOne.CreationCommand.copy(username = "game-service-test-1")
    playerService.create(cmd).right.value
    playerService.create(cmd).left.value must matchPattern { case models.NotUniqueError(_) => }
  }

  it should "fetch a player successfully given an existent ID" in { implicit session =>
    val playerId = playerService.create(PlayerExamples.PlayerOne.CreationCommand.copy(username = "player-service-test-2")).right.value.id
    val result = playerService.findById(playerId)
    val player = result.right.value
    player.id must be(playerId)
    player.username must be("player-service-test-2")
  }

  it should "not fetch result for a nonexistent ID" in { implicit session =>
    val result = playerService.findById(909090).left.value
    result must matchPattern { case models.ResourceNotFound(_) => }
  }

  it should "mark as deleted a player successfully given an existent ID" in { implicit session =>
    val playerId = playerService.create(PlayerExamples.PlayerOne.CreationCommand.copy(username = "player-service-test-3")).right.value.id
    val result = playerService.deactivate(playerId)
    val _: Unit = result.right.value

    val value1 = playerService.findById(playerId)
    val newResult = value1.left.value
    newResult must matchPattern { case models.ResourceNotFound(_) => }
  }
}
