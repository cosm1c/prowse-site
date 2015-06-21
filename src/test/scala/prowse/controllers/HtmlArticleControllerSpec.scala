package prowse.controllers

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import prowse.domain.HtmlArticleRepository
import prowse.domain.HtmlArticleStubs._

import scala.concurrent.Future


class HtmlArticleControllerSpec extends Specification with Mockito {

  "HtmlArticleController" should {

    "delegate to  injected repository" in {
      val mockHtmlArticleRepository = mock[HtmlArticleRepository]
      val htmlArticleController = new HtmlArticleController(mockHtmlArticleRepository)
      val path = "somePath"

      mockHtmlArticleRepository.findByPath(path) returns Future.successful(None)

      htmlArticleController.get(path).apply(FakeRequest())

      there was one(mockHtmlArticleRepository).findByPath(path)
    }

    "provide response from repository as request content" in {
      val mockHtmlArticleRepository = mock[HtmlArticleRepository]
      val htmlArticleController = new HtmlArticleController(mockHtmlArticleRepository)
      val path = "somePath"

      mockHtmlArticleRepository.findByPath(path) returns Future.successful(Some(simpleTextArticle))

      val eventualResult: Future[Result] = htmlArticleController.get(path).apply(FakeRequest())

      contentAsString(eventualResult) === simpleTextArticleHtml.toString()
    }
  }

}
