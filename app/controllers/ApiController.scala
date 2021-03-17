package controllers

import controllers.response._
import models._
import play.api.Logging
import play.api.mvc.{BaseController, Result}
import support.json.{JsonSupport, PlayJsonExtension}

abstract class ApiController extends BaseController with JsonSupport with PlayJsonExtension {
  self: Logging =>

  def handleAppError[T](appResult: AppResult[T], operation: String = "operation", entity: String = "entity")(f: T => Result): Result = {
    appResult match {
      case Right(value) => f(value)
      case Left(err: InvalidParametersError) =>
        logger.error(s"error while $operation. Reason: ${err.reason}", err)
        UnprocessableEntity(UnprocessableResponse(s"Invalid parameter. Reason: ${err.reason}", ErrorCode.ValidationError).asJson)
      case Left(err: InvalidStateTransitionError) =>
        BadRequest(BadRequestResponse(s"$entity cannot transit the state. Reason: ${err.reason}", ErrorCode.ClientError).asJson)
      case Left(err: ClientError) =>
        BadRequest(BadRequestResponse(s"Reason: ${err.reason}", ErrorCode.ClientError).asJson)
      case Left(err: ResourceNotFound) =>
        NotFound(NotFoundResponse(s"$entity cannot be found. Reason: ${err.reason}", ErrorCode.NotFound).asJson)
      case Left(err: NotUniqueError) =>
        Conflict(BadRequestResponse(s"$entity is not unique. Reason: ${err.reason}", ErrorCode.AlreadyExists).asJson)
      case Left(err) =>
        logger.error(s"Error while $operation. Reason: ${err.reason}", err)
        InternalServerError(InternalServerErrorResponse(s"Unexpected error. Reason: ${err.reason}", ErrorCode.InternalError).asJson)
    }
  }
}
