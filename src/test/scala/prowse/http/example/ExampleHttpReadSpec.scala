package prowse.http.example

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import play.api.test.{FakeApplication, Helpers, TestServer}
import prowse.http._
import prowse.service.MockTimeService

import scala.language.reflectiveCalls

class ExampleHttpReadSpec extends HttpReadSpecification with PlayServerRunning with HttpHelpers {

  private val missingPathString: String = "missing"
  override val okPath: String = "exists"
  override val missingPath: Option[String] = Some(missingPathString)
  implicit val timeService = MockTimeService

  private val controller = new Controller {
    def exists = Action {
      timeService.dateHeader {
        Ok(
          Json.obj(
            "hello" -> "world"
          )
        )
      }
    }

    def missing = Action {
      timeService.dateHeader {
        NotFound
      }
    }
  }

  override def createTestServer = TestServer(Helpers.testServerPort, application = FakeApplication(
    withRoutes = {
      case ("GET", path) if "/" + okPath == path => controller.exists
      case ("HEAD", path) if "/" + okPath == path => controller.exists
      case ("GET", path) if "/" + missingPathString == path => controller.missing
      case ("HEAD", path) if "/" + missingPathString == path => controller.missing
    }
  ))

}
