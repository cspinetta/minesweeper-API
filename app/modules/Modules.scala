package modules

import java.time.Clock

import com.google.inject.AbstractModule

class Modules extends AbstractModule {

  override def configure(): Unit = {
    // Use the system clock as the default implementation of Clock
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    bind(classOf[ApplicationStartup]).asEagerSingleton()
  }
}
