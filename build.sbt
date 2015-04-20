import com.typesafe.sbt.SbtNativePackager.NativePackagerKeys._
import com.typesafe.sbt.SbtNativePackager._

name := """prowse-site"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin, PlayScala).
  settings(
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

libraryDependencies ++= Seq(
  ws,
  "com.google.inject" % "guice" % "3.0",
  "javax.inject" % "javax.inject" % "1",
  "org.mockito" % "mockito-core" % "1.10.19" % "test"
)

packageArchetype.java_server

maintainer in Docker := "Cory Prowse <cory@prowse.com>"

dockerBaseImage in Docker := "dockerfile/java:oracle-java8"

dockerExposedPorts in Docker := Seq(9000, 9443)

packageSummary in Docker := "Prowse website"

packageDescription := "A test area for computer technology."

buildInfoOptions += BuildInfoOption.ToJson
