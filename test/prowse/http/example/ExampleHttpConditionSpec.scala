package prowse.http.example

import java.time.ZonedDateTime

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Controller
import play.api.test.{FakeApplication, Helpers, TestServer}
import prowse.http._

import scala.language.reflectiveCalls

class ExampleHttpConditionSpec extends HttpConditionSpecification with PlayServerRunning with HttpHelpers {

  private val lastModified = ZonedDateTime.now
  private val missingPathString: String = "missing"
  override val okPath: String = "exists"
  override val missingPath: Option[String] = Some(missingPathString)

  private val controller = new Controller with PlayCacheable {
    def exists = conditionalAction(
      Some(StaticCacheableContent(
        StrongETag("ETAGVALUE", "\"ETAGVALUE\""),
        lastModified,
        Json.obj(
          "hello" -> "world"
        )
      )))

    def missing = conditionalAction(missingContent)

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
