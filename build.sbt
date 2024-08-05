ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "ru.ekuzmichev"
ThisBuild / scalaVersion := "3.3.3"

val zioVersion = "4.0.2"

ThisBuild / assemblyMergeStrategy := {
  case PathList("app.local.conf")            => MergeStrategy.discard
  case PathList("logback.xml")               => MergeStrategy.discard
  case PathList("logback-test.xml")          => MergeStrategy.discard
  case x if x.endsWith("module-info.class")  => MergeStrategy.concat
  case x if x.endsWith("okio.kotlin_module") => MergeStrategy.concat
  case x                                     => MergeStrategy.defaultMergeStrategy(x)
}

lazy val root = (project in file("."))
  .settings(
    name                                          := "ozon-price-checker",
    idePackagePrefix.withRank(KeyRanks.Invisible) := Some("ru.ekuzmichev"),
    assembly / mainClass                          := Some("ru.ekuzmichev.app.OzonPriceCheckerApp"),
    assembly / assemblyJarName                    := s"${name.value}-${version.value}.jar",
    libraryDependencies ++= Seq(
      "net.ruippeixotog"              %% "scala-scraper"            % "3.1.1",
      "org.telegram"                   % "telegrambots-client"      % "7.7.1",
      "org.telegram"                   % "telegrambots-longpolling" % "7.7.1",
      "ch.qos.logback"                 % "logback-classic"          % "1.5.6",
      "dev.zio"                       %% "zio"                      % "2.1.6",
      "dev.zio"                       %% "zio-config"               % zioVersion,
      "dev.zio"                       %% "zio-config-magnolia"      % zioVersion,
      "dev.zio"                       %% "zio-config-typesafe"      % zioVersion,
      "dev.zio"                       %% "zio-logging-slf4j2"       % "2.3.0",
      "com.github.alonsodomin.cron4s" %% "cron4s-core"              % "0.7.0",
      "org.typelevel"                 %% "cats-core"                % "2.12.0",
      "io.lemonlabs"                  %% "scala-uri"                % "4.0.3",
      "org.scalatest"                 %% "scalatest"                % "3.2.19" % Test,
      "dev.zio"                       %% "zio-test"                 % "2.1.6"  % Test
    )
  )
