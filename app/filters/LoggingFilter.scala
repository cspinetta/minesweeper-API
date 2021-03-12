package filters

import akka.stream.Materializer
import javax.inject.Inject
import play.api.Logging
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class LoggingFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter with Logging {

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {

    val startTime = System.currentTimeMillis

    nextFilter(requestHeader).map { result =>
      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime

      val msg = s"${requestHeader.method} ${requestHeader.uri} took ${requestTime}ms and returned ${result.header.status}"

      if (LoggingFilter.hiddenEndpoints.contains(requestHeader.uri)) {
        logger.trace(msg)
      } else {
        logger.info(msg)
      }

      result.withHeaders("Request-Time" -> requestTime.toString)
    }
  }
}

object LoggingFilter {

  val hiddenEndpoints: Set[String] = Set(
    controllers.routes.HealthCheckController.healthCheck().url
  )
}
