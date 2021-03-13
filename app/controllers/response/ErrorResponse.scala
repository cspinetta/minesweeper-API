package controllers.response

import play.api.libs.json.{Json, Writes}

/**
 * Model representation for a response error
 */
sealed trait ErrorResponse {
  def error: String

  def message: String

  def errorCode: String
}

case class NotFoundResponse(message: String, errorCode: String) extends ErrorResponse {
  val error: String = "not_found"
}

object NotFoundResponse {
  implicit val writeNotFoundResponse: Writes[NotFoundResponse] = Json.writes[NotFoundResponse]
}

case class BadRequestResponse(message: String, errorCode: String) extends ErrorResponse {
  val error: String = "bad_request"
}

object BadRequestResponse {
  implicit val writeBadRequestResponse: Writes[BadRequestResponse] = Json.writes[BadRequestResponse]
}

case class InternalServerErrorResponse(message: String, errorCode: String) extends ErrorResponse {
  val error: String = "internal_server_error"
}

object InternalServerErrorResponse {
  implicit val writeInternalServerErrorResponse: Writes[InternalServerErrorResponse] = Json.writes[InternalServerErrorResponse]
}
