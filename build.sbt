ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3"

lazy val root = (project in file("."))
  .settings(
    name := "ozon-price-checker",
    idePackagePrefix := Some("ru.ekuzmichev"),
    libraryDependencies ++= Seq(
      "net.ruippeixotog" %% "scala-scraper" % "3.1.1"
    )
  )
