package prowse.http

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.language.postfixOps

class HttpReadPerf extends Simulation {

  val pathsThatExist = Seq(
    "/article/loremIpsum",
    "/buildInfo.html",
    "/buildInfo"
  )

  val pathsThatDoNotExist = Seq(
    "/article/missingPath"
  )

  // TODO: update default headers with actual values from a browser
  val httpConf = http
    .baseURL("http://192.168.59.103:9000")
    .acceptEncodingHeader("gzip, deflate")
    .disableCaching

  def scnHttpRead =
    scenario("Http Read Specification - https://tools.ietf.org/html/rfc7231")
      .foreach(pathsThatExist, HttpSemanticsAndContents.PATH_KEY)(
        repeat(10, "n") {
          exec(HttpSemanticsAndContents.withExistingPaths)
        })
      .foreach(pathsThatDoNotExist, HttpSemanticsAndContents.PATH_KEY)(
        repeat(10, "n") {
          exec(HttpSemanticsAndContents.withMissingPaths)
        }
      )

  setUp(
    scnHttpRead.inject(
      atOnceUsers(1000)
    ).protocols(httpConf)
  )
}
