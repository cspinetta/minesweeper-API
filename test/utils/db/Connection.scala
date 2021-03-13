package utils.db

import scalikejdbc.{ConnectionPool, ConnectionPoolSettings}

trait Connection {
  Class.forName("org.h2.Driver")
  val settings: ConnectionPoolSettings = ConnectionPoolSettings(
    initialSize = 0,
    maxSize = 8,
    connectionTimeoutMillis = 5000L,
    validationQuery = null,
    connectionPoolFactoryName = null,
    driverName = null,
    warmUpTime = 100L,
    timeZone = "UTC"
  )
  ConnectionPool.singleton("jdbc:h2:mem:minesweeper-api;MODE=MYSQL;INIT=RUNSCRIPT FROM 'classpath:scripts/init-db-h2.sql'", "", "", settings)
}
