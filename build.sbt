import com.typesafe.sbt.SbtNativePackager.autoImport._
import play.sbt.PlayImport._
import sbt.Keys._


lazy val commonSettings = Seq(
  version := "1.0-SNAPSHOT",

  scalaVersion := "2.11.6",

  scalacOptions ++= Seq(
    "-target:jvm-1.8",
    "-encoding", "UTF-8",
    "-deprecation", // warning and location for usages of deprecated APIs
    "-feature", // warning and location for usages of features that should be imported explicitly
    "-unchecked", // additional warnings where generated code depends on assumptions
    "-Xlint", // recommended additional warnings
    "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
    "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
    "-Ywarn-inaccessible",
    "-Ywarn-dead-code"
  ),

  libraryDependencies ++= Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
    "nl.grons" %% "metrics-scala" % "3.5.1_a2.3",
    "io.dropwizard.metrics" % "metrics-graphite" % "3.1.2"
  ),

  // Build Info
  buildInfoOptions += BuildInfoOption.ToJson,
  buildInfoPackage := "prowse.domain",
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    sbtVersion,
    BuildInfoKey.action("buildInstant") {
      java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now())
    },
    BuildInfoKey.action("gitChecksum") {
      // git describe would be better but requires annotations exist
      com.typesafe.sbt.git.ConsoleGitRunner.apply("rev-parse", "HEAD")(new java.io.File(System.getProperty("user.dir")))
    })
)

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin, PlayScala)
  .disablePlugins(PlayLayoutPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := """prowse-site""",

    packageDescription := "A test area for computer technology.",

    // -Yrangepos is for specs2
    scalacOptions in Test ++= Seq("-Yrangepos"),

    resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",

    libraryDependencies ++= Seq(
      ws % Test,
      "org.mockito" % "mockito-core" % "1.10.19" % "test",
      specs2 % Test,
      "org.specs2" % "specs2-matcher-extra_2.11" % "3.6" % "test"
    ),

    routesGenerator := InjectedRoutesGenerator,

    // Docker
    packageSummary := "Prowse website",
    maintainer := "Cory Prowse <cory@prowse.com>",
    dockerBaseImage := "ingensi/oracle-jdk",
    dockerExposedPorts := Seq(9000, 9443)
  )

lazy val gatling = (project in file("gatling"))
  .enablePlugins(GatlingPlugin, BuildInfoPlugin, PlayScala)
  .disablePlugins(PlayLayoutPlugin)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.1.6",
      "io.gatling" % "gatling-test-framework" % "2.1.6"
    )
  )
  .dependsOn(root)
