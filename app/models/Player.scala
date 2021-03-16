package models

import java.time.ZonedDateTime

case class PlayerCreationCommand(username: String, password: String)

case class PlayerDeletionCommand(id: Long)

case class Player(id: Long,
                  username: String,
                  encodedPass: String,
                  createdAt: ZonedDateTime,
                  deletedAt: Option[ZonedDateTime] = None)
