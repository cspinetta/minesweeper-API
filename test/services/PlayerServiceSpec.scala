package services

import examples.PlayerExamples
import org.scalatest.{EitherValues, MustMatchers, fixture}
import repositories.PlayerRepository
import scalikejdbc._
import utils.db.{AutoRollback, Connection}

class PlayerServiceSpec extends fixture.FlatSpec with Connection with AutoRollback with MustMatchers with EitherValues {

  val playerRepository = new PlayerRepository
  val playerService = new PlayerService(playerRepository)

  override def fixture(implicit session: DBSession) {
    sql"insert into player values (1, 'Alice', NOW(), null)".update.apply()
    sql"insert into player values (2, 'Bob', NOW(), null)".update.apply()
  }

  behavior of "Player service"

  it should "create a new player successfully" in { implicit session =>
    val cmd = PlayerExamples.PlayerOne.CreationCommand
    val result = playerService.create(cmd)
    val player = result.right.value
    player.id mustBe >(0L)
    player.username must be(cmd.username)
  }

  it should "fetch a player successfully given an existent ID" in { implicit session =>
    val result = playerService.findById(1)
    val player = result.right.value
    player.id must be(1L)
    player.username must be("Alice")
  }

  it should "mark as deleted a player successfully given an existent ID" in { implicit session =>
    val result = playerService.deactivate(1)
    val _: Unit = result.right.value

    val value1 = playerService.findById(1)
    val newResult = value1.left.value
    newResult must matchPattern { case models.ResourceNotFound(_) => }
  }
}
