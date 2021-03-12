package controllers

import com.github.tototoshi.play2.json4s.Json4s
import controllers.response.HealthCheckResponse
import javax.inject._
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HealthCheckController @Inject()(val controllerComponents: ControllerComponents,
                                      val json4s: Json4s)
  extends ApiController {

  val healthCheckResponse: HealthCheckResponse = HealthCheckResponse(
    status = "OK"
  )

  /**
   * Inform App health
   *
   * @return 200 OK - app is OK
   */
  def healthCheck(): Action[AnyContent] = Action { _ =>
    Ok(healthCheckResponse.asJson) as "application/json"
  }

}
