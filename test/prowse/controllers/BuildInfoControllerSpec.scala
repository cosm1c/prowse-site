package prowse.controllers

import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import play.api.mvc.Result
import play.api.test.{FakeRequest, PlaySpecification, WithServer}

import scala.concurrent.Future

class BuildInfoControllerSpec extends PlaySpecification {

  val buildInfoController = new BuildInfoController()

  "BuildInfoController" should {

    "display current build info as JSON" in {
      val result: Future[Result] = buildInfoController.getBuildInfoJson().apply(FakeRequest())
      contentAsJson(result).toString() mustEqual Json.stringify(buildInfoController.jsonResponseContent)
    }

    "run in a server at path 'buildInfo'" in new WithServer {
      val response: WSResponse = await(WS.url(s"http://localhost:$port/buildInfo").get())
      (response.status mustEqual OK) and
        (response.json mustEqual buildInfoController.jsonResponseContent)
    }

    "run in a server at path 'buildInfo.html'" in new WithServer {
      val response: WSResponse = await(WS.url(s"http://localhost:$port/buildInfo.html").get())
      response.status mustEqual OK
    }
  }

}
