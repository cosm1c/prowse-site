package prowse.controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.Controller
import prowse.domain.HtmlArticleRepository
import prowse.domain.HtmlArticleRepository.articleToCacheableHtml
import prowse.http.PlayCacheable._

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class HtmlArticleController @Inject()(htmlArticleRepository: HtmlArticleRepository) extends Controller {

  def get(path: String) = conditionalAsyncAction(
    htmlArticleRepository.findByPath(path).map(maybeArticle =>
      maybeArticle.map(articleToCacheableHtml)
    )
  )

}
