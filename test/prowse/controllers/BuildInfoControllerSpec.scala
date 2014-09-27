package prowse.controllers

import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import play.api.mvc.Result
import play.api.test.{FakeRequest, PlaySpecification, WithServer}

import scala.concurrent.Future

class BuildInfoControllerSpec extends PlaySpecification {

  "BuildInfoController" should {

    "display current build info as JSON" in {
      val result: Future[Result] = BuildInfoController.getBuildInfoJson().apply(FakeRequest())
      contentAsJson(result).toString() mustEqual Json.stringify(BuildInfoController.jsonResponseContent)
    }

    "run in a server at path 'buildInfo'" in new WithServer {
      val response: WSResponse = await(WS.url(s"http://localhost:$port/buildInfo").get())
      (response.status mustEqual OK) and
        (response.json mustEqual BuildInfoController.jsonResponseContent)
    }
  }

}
