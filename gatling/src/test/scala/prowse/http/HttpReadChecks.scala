package prowse.http

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import play.api.test.{Helpers, TestServer}

class HttpReadChecks extends Simulation {

  val pathsThatExist = Seq(
    "/article/loremIpsum",
    "/buildInfo.html"
  )

  val pathsThatDoNotExist = Seq(
    "/article/missingPath"
  )

  val server = TestServer(Helpers.testServerPort)

  before {
    server.start()
  }

  after {
    server.stop()
  }

  // TODO: update default headers with actual values from a browser
  val httpConf = http
    .baseURL("http://localhost:" + Helpers.testServerPort)
    .acceptEncodingHeader("gzip, deflate")
    .disableCaching

  def scnHttpRead =
    scenario("Http Read Specification - https://tools.ietf.org/html/rfc7231")
      .foreach(pathsThatExist, HttpSemanticsAndContents.PATH_KEY)(exec(HttpSemanticsAndContents.withExistingPaths))
      .foreach(pathsThatDoNotExist, HttpSemanticsAndContents.PATH_KEY)(exec(HttpSemanticsAndContents.withMissingPaths))

  setUp(
    scnHttpRead.inject(
      atOnceUsers(1)
    ).protocols(httpConf)
  )
}