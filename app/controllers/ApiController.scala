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
        UnprocessableEntity(UnprocessableResponse(s"invalid parameter", ErrorCode.ValidationError).asJson)
      case Left(_: InvalidStateTransitionError) =>
        BadRequest(BadRequestResponse(s"$entity cannot transit the state", ErrorCode.ClientError).asJson)
      case Left(_: ResourceNotFound) =>
        NotFound(NotFoundResponse(s"$entity cannot be found", ErrorCode.NotFound).asJson)
      case Left(_: NotUniqueError) =>
        Conflict(BadRequestResponse(s"$entity is not unique", ErrorCode.AlreadyExists).asJson)
      case Left(err) =>
        logger.error(s"error while $operation. Reason: ${err.reason}", err)
        InternalServerError(InternalServerErrorResponse("unexpected error", ErrorCode.InternalError).asJson)
    }
  }
}
