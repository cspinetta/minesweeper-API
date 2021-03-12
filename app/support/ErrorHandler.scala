package support

import controllers.response.{BadRequestResponse, InternalServerErrorResponse, NotFoundResponse}
import javax.inject.Singleton
import play.api.Logging
import play.api.http.HttpErrorHandler
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

@Singleton
class ErrorHandler extends HttpErrorHandler with Logging {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    statusCode match {
      case NOT_FOUND => Future.successful(NotFound(Json.toJson(NotFoundResponse(message, "C001"))))
      case BAD_REQUEST => Future.successful(NotFound(Json.toJson(BadRequestResponse(message, "C003"))))
      case _ => Future.successful(Status(statusCode))
    }
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    logger.error("Unexpected error ", exception)
    Future.successful(InternalServerError(Json.toJson(InternalServerErrorResponse(exception.getMessage, "C002"))))
  }
}
