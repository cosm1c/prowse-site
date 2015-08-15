package prowse.http.example

import java.time.ZoneId

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Controller
import play.api.test.{FakeApplication, Helpers, TestServer}
import prowse.http.Cacheable._
import prowse.http._
import prowse.service.MockTimeService

import scala.concurrent.Future
import scala.language.reflectiveCalls

class ExampleHttpConditionSpec extends HttpConditionSpecification with PlayServerRunning with HttpHelpers {

  private val missingPathString: String = "missing"
  override val okPath: String = "exists"
  override val missingPath: Option[String] = Some(missingPathString)
  implicit val timeService = MockTimeService
  private val lastModified = timeService.clock.instant().atZone(ZoneId.of("GMT"))

  private val controller = new Controller {
    def exists = CacheableAction(
      Future.successful(
        Some(
          StaticCacheableContent(
            StrongETag("ETAGVALUE"),
            lastModified,
            Json.obj(
              "hello" -> "world"
            )
          )
        )
      )
    )

    def missing = CacheableAction(Future.successful(missingContent))

    private def missingContent: Option[CacheableContent[JsValue]] = None
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
