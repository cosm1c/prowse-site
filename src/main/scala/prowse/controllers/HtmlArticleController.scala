package prowse.controllers

import javax.inject.{Inject, Singleton}

import nl.grons.metrics.scala.Timer
import play.api.mvc.{Action, AnyContent, Controller}
import prowse.domain.HtmlArticleRepository
import prowse.domain.HtmlArticleRepository.articleToCacheableHtml
import prowse.http.PlayCacheable._
import prowse.metrics.Instrumented

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class HtmlArticleController @Inject()(htmlArticleRepository: HtmlArticleRepository) extends Controller with Instrumented {

  val get: Timer = metrics.timer("get")

  def get(path: String): Action[AnyContent] = get.time {
    conditionalAsyncAction(
      htmlArticleRepository.findByPath(path).map(maybeArticle =>
        maybeArticle.map(articleToCacheableHtml)
      )
    )
  }

}
