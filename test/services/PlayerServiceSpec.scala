package services

import examples.PlayerExamples
import models.PlayerCreationCommand
import org.scalatest.{EitherValues, MustMatchers, fixture}
import repositories.PlayerRepository
import scalikejdbc._
import utils.db.{AutoRollback, Connection}

class PlayerServiceSpec extends fixture.FlatSpec with Connection with AutoRollback with MustMatchers with EitherValues {

  val playerRepository = new PlayerRepository
  val playerService = new PlayerService(playerRepository)

  override def fixture(implicit session: DBSession) {
    sql"insert into player values (1, 'alice', NOW(), null)".update.apply()
    sql"insert into player values (2, 'bob', NOW(), null)".update.apply()
  }

  behavior of "Player service"

  it should "create a new player successfully" in { implicit session =>
    val cmd = PlayerExamples.PlayerOne.CreationCommand
    val result = playerService.create(cmd)
    val player = result.right.value
    player.id mustBe >(0L)
    player.username must be(cmd.username)
  }

  it should "not create a new player when exists another one with the same username" in { implicit session =>
    val cmd = PlayerCreationCommand(username = "alice")
    val result = playerService.create(cmd).left.value
    result must matchPattern { case models.NotUniqueError(_) => }
  }

  it should "fetch a player successfully given an existent ID" in { implicit session =>
    val result = playerService.findById(1)
    val player = result.right.value
    player.id must be(1L)
    player.username must be("alice")
  }

  it should "not fetch result for a nonexistent ID" in { implicit session =>
    val result = playerService.findById(909090).left.value
    result must matchPattern { case models.ResourceNotFound(_) => }
  }

  it should "mark as deleted a player successfully given an existent ID" in { implicit session =>
    val result = playerService.deactivate(1)
    val _: Unit = result.right.value

    val value1 = playerService.findById(1)
    val newResult = value1.left.value
    newResult must matchPattern { case models.ResourceNotFound(_) => }
  }
}
