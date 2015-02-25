import com.typesafe.sbt.SbtNativePackager.NativePackagerKeys._
import com.typesafe.sbt.SbtNativePackager._

name := """prowse-site"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  ws,
  "com.google.inject" % "guice" % "3.0",
  "javax.inject" % "javax.inject" % "1",
  "org.mockito" % "mockito-core" % "1.10.19" % "test"
)

packageArchetype.java_server

maintainer := "Cory Prowse <cory@prowse.com>"

dockerBaseImage := "dockerfile/java:oracle-java8"

dockerExposedPorts in Docker := Seq(9000, 9443)

//------------------------------------------------------------------------------

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq[BuildInfoKey](
  name,
  version,
  BuildInfoKey.action("buildInstant") {
    java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now())
  },
  BuildInfoKey.action("gitChecksum") {
    com.typesafe.sbt.git.ConsoleGitRunner.apply("rev-parse", "HEAD")(new java.io.File(System.getProperty("user.dir")))
  }
)
