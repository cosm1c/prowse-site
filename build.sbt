name := """prowse-site"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin, PlayScala)
  .disablePlugins(PlayLayoutPlugin)
  .settings(
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
        com.typesafe.sbt.git.ConsoleGitRunner.apply("rev-parse", "HEAD")(new java.io.File(System.getProperty("user.dir")))
      })
  )

scalaVersion := "2.11.6"

// -Yrangepos is for specs2
scalacOptions in Test ++= Seq("-Yrangepos")

//enablePlugins(JavaAppPackaging)

libraryDependencies ++= Seq(
  ws,
  "org.mockito" % "mockito-core" % "1.10.19" % "test"
)

libraryDependencies += specs2 % Test

libraryDependencies += "org.specs2" % "specs2-matcher-extra_2.11" % "3.6"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

maintainer in Docker := "Cory Prowse <cory@prowse.com>"

dockerBaseImage in Docker := "dockerfile/java:oracle-java8"

dockerExposedPorts in Docker := Seq(9000, 9443)

packageSummary in Docker := "Prowse website"

packageDescription := "A test area for computer technology."

buildInfoOptions += BuildInfoOption.ToJson
