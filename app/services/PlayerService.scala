package services

import javax.inject.{Inject, Singleton}
import models._
import play.api.Logging
import repositories.PlayerRepository
import scalikejdbc.DBSession

@Singleton
class PlayerService @Inject()(val playerRepository: PlayerRepository) extends Logging {

  def create(c: PlayerCreationCommand)(implicit session: DBSession): Either[AppError, Player] = {
    playerRepository.create(c)
  }

  def deactivate(id: Long)(implicit session: DBSession): Either[AppError, Unit] = {
    playerRepository.deactivate(id)
  }

  def findById(id: Long)(implicit session: DBSession): Either[AppError, Player] = {
    playerRepository.find(id)
  }
}
