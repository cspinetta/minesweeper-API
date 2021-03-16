package support.auth

import java.security.MessageDigest
import java.util.Base64

import controllers.response.ErrorCode
import javax.inject.Inject
import models.{AppError, Player, ResourceNotFound}
import play.api.http.ContentTypes.JSON
import play.api.mvc.Results.Unauthorized
import play.api.mvc.Security.AuthenticatedBuilder
import play.api.mvc._
import services.PlayerService
import support.db.TxSupport

import scala.concurrent.ExecutionContext

case class User(userId: Long, user: String)

class UserAuthenticatedBuilder @Inject()(parser: BodyParsers.Default, playerService: PlayerService)(implicit ec: ExecutionContext)
  extends AuthenticatedBuilder[User](AuthUtils.validateIncomingUser(playerService), parser, onUnauthorized = AuthUtils.onUnauthorized)

object AuthUtils {
  def hash(password: String): String = new String(MessageDigest.getInstance("MD5").digest(password.getBytes))

  def onUnauthorized(req: RequestHeader): Result = {
    Unauthorized(
      s"""{
         |  "message": "unauthorized request",
         |  "error_code": "${ErrorCode.Unauthorized}"
         |}""".stripMargin) as JSON
  }

  def validateIncomingUser(playerService: PlayerService)(req: RequestHeader): Option[User] = {
    req.headers.get("Authorization") flatMap { authHeader =>
      val (user, pass) = decodeBasicAuth(authHeader)
      TxSupport.withinReadOnlyTx[AppError, Player](session => playerService.findByCredentials(user, AuthUtils.hash(pass))(session)) match {
        case Right(player) => Some(User(userId = player.id, user = player.username))
        case Left(ResourceNotFound(_)) => None
        case Left(_) => None
      }
    }
  }

  private[this] def decodeBasicAuth(authHeader: String): (String, String) = {
    val baStr = authHeader.replaceFirst("Basic ", "")
    val decoded = Base64.getDecoder.decode(baStr)
    val Array(user, password) = new String(decoded).split(":")
    (user, password)
  }
}
