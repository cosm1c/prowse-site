name := """prowse-site"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  ws,
  "com.google.inject" % "guice" % "3.0",
  "javax.inject" % "javax.inject" % "1",
  "org.mockito" % "mockito-core" % "1.10.19" % "test"
)

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
