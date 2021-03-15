package conf

import com.typesafe.config.Config
import conf.AppConfig.values.GameConfig
import javax.inject.{Inject, Singleton}
import play.api.{ConfigLoader, Configuration}


@Singleton
class AppConfigProvider @Inject()(val config: Configuration) {

  val app: AppConfig = config.get[AppConfig](path = "app")
}

case class AppConfig(environment: String, game: GameConfig)

object AppConfig {
  // DO NOT REMOVE THIS IMPORT
  import pureconfig._
  import pureconfig.generic.auto._

  implicit val configLoader: ConfigLoader[AppConfig] = (rootConfig: Config, path: String) => {
    val config = rootConfig.getConfig(path)
    ConfigSource.fromConfig(config).loadOrThrow[AppConfig]
  }

  object values {

    case class GameConfig(maxHeight: Int, maxWidth: Int)

  }

}
