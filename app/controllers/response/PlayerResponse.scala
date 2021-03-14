package controllers.response

import java.time.ZonedDateTime

import models.Player

case class PlayerResponse(id: Long, username: String, createdAt: ZonedDateTime)

object PlayerResponse {
  def apply(player: Player): PlayerResponse = PlayerResponse(player.id, player.username, player.createdAt)
}
