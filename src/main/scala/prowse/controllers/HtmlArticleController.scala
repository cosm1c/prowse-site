package prowse.controllers

import javax.inject.{Inject, Singleton}

import nl.grons.metrics.scala.Timer
import play.api.mvc.Controller
import prowse.domain.HtmlArticleRepository
import prowse.domain.HtmlArticleRepository.articleToCacheableHtml
import prowse.http.CacheableAction
import prowse.metrics.Instrumented
import prowse.service.TimeService

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class HtmlArticleController @Inject()(htmlArticleRepository: HtmlArticleRepository, implicit val timeService: TimeService) extends Controller with Instrumented {

  val timer: Timer = metrics.timer("get-article")

  def get(path: String) = CacheableAction {
    timer.time(
      htmlArticleRepository.findByPath(path).map(maybeArticle =>
        maybeArticle.map(articleToCacheableHtml)
      )
    )
  }

}
