package models

import java.time.ZonedDateTime

case class PlayerCreationCommand(username: String)
case class PlayerDeletionCommand(id: Long)

case class Player(id: Long,
                  username: String,
                  createdAt: ZonedDateTime,
                  deletedAt: Option[ZonedDateTime] = None)
