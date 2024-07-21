ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3"

val zioVersion = "4.0.2"

lazy val root = (project in file("."))
  .settings(
    name             := "ozon-price-checker",
    idePackagePrefix := Some("ru.ekuzmichev"),
    libraryDependencies ++= Seq(
      "net.ruippeixotog" %% "scala-scraper"            % "3.1.1",
      "org.telegram"      % "telegrambots-longpolling" % "7.7.1",
      "ch.qos.logback"    % "logback-classic"          % "1.5.6",
      "dev.zio"          %% "zio"                      % "2.1.6",
      "dev.zio"          %% "zio-config"               % zioVersion,
      "dev.zio"          %% "zio-config-magnolia"      % zioVersion,
      "dev.zio"          %% "zio-config-typesafe"      % zioVersion,
      "dev.zio"          %% "zio-logging-slf4j2"       % "2.3.0"
    )
  )
