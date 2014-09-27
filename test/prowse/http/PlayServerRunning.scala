package prowse.http

import org.specs2.SpecificationLike
import org.specs2.specification.{Fragments, Step}
import play.api.test.{Helpers, TestServer}

trait PlayServerRunning extends SpecificationLike {

  override def map(fs: => Fragments): Fragments = Step(beforeAll) ^ fs ^ Step(afterAll)

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
