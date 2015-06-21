package prowse.http

import org.specs2.SpecificationLike
import org.specs2.specification.core.Fragments
import play.api.test.{Helpers, TestServer}

trait PlayServerRunning extends SpecificationLike {

  override def map(fs: => Fragments): Fragments = step(beforeAll()) ^ fs ^ step(afterAll())

  private lazy val server = createTestServer

  // override if needed
  protected def createTestServer: TestServer = TestServer(Helpers.testServerPort)

  private def beforeAll() = {
    server.start()
  }

  private def afterAll() = {
    server.stop()
  }

}
