package controllers.response

import play.api.libs.json.{Json, Writes}

/**
 * Model representation for a response error
 */
sealed trait ErrorResponse {
  def message: String

  def errorCode: String
}

case class NotFoundResponse(message: String, errorCode: String) extends ErrorResponse

object NotFoundResponse {
  implicit val writeNotFoundResponse: Writes[NotFoundResponse] = Json.writes[NotFoundResponse]
}

case class BadRequestResponse(message: String, errorCode: String) extends ErrorResponse

object BadRequestResponse {
  implicit val writeBadRequestResponse: Writes[BadRequestResponse] = Json.writes[BadRequestResponse]
}

case class UnprocessableResponse(message: String, errorCode: String) extends ErrorResponse

object UnprocessableResponse {
  implicit val writeUnprocessableResponse: Writes[UnprocessableResponse] = Json.writes[UnprocessableResponse]
}

case class UnauthorizedResponse(message: String, errorCode: String) extends ErrorResponse

object UnauthorizedResponse {
  implicit val writeUnauthorizedResponse: Writes[UnauthorizedResponse] = Json.writes[UnauthorizedResponse]
}

case class InternalServerErrorResponse(message: String, errorCode: String) extends ErrorResponse

object InternalServerErrorResponse {
  implicit val writeInternalServerErrorResponse: Writes[InternalServerErrorResponse] = Json.writes[InternalServerErrorResponse]
}
