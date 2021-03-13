val appName = "minesweeper-api"

val catsVersion = "2.1.1"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SwaggerPlugin)
  .settings(
    name := appName,
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.13.4",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-unchecked",
      "-Xlint",
      "-Ywarn-numeric-widen",
      //      "-Xfatal-warnings",
    ),
    scalacOptions in Compile ++= Seq(
      "-Ywarn-dead-code",
    ),
    PlayKeys.devSettings += "config.file" -> "conf/application.dev.conf",
    swaggerDomainNameSpaces := Seq("models", "controllers.response"),
    libraryDependencies ++= Seq(
      guice,
      "com.h2database" % "h2" % "1.4.200",
      "com.github.pureconfig" %% "pureconfig" % "0.14.1",
      "org.typelevel" %% "cats-core" % catsVersion,
      "com.beachape" %% "enumeratum" % "1.6.1",
      "com.beachape" %% "enumeratum-json4s" % "1.6.0",
      "com.h2database" % "h2" % "1.4.200",
      "org.scalikejdbc" %% "scalikejdbc" % "3.5.0",
      "org.scalikejdbc" %% "scalikejdbc-config" % "3.5.0",
      "org.scalikejdbc" %% "scalikejdbc-play-initializer" % "2.8.0-scalikejdbc-3.5",
      "org.json4s" %% "json4s-core" % "3.6.10",
      "org.json4s" %% "json4s-ext" % "3.6.10",
      "com.github.tototoshi" %% "play-json4s-jackson" % "0.10.1",
      "org.webjars" % "swagger-ui" % "3.43.0",
      "com.github.tototoshi" %% "play-json4s-test-jackson" % "0.10.1" % Test,
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
    ),
    coverageEnabled in(Test, compile) := true,
    coverageExcludedPackages := "<empty>;Reverse.*;router\\.*;support.executor.*;support.json.*;controllers.javascript;" +
      appName + "filters.LoggingFilter;.*Error.*;controllers.response.BadRequestResponse;conf.*;",
  )
