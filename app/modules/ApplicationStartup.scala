package modules

import conf.AppConfigProvider
import javax.inject.Inject
import play.api.Logging
import scalikejdbc.DB

class ApplicationStartup @Inject()(val config: AppConfigProvider) extends Logging {

//  if (config.app.environment.equalsIgnoreCase("dev")) {
//  }
}
