package prowse.controllers

import javax.inject.Singleton

import nl.grons.metrics.scala.Timer
import play.api.mvc.Controller
import prowse.domain.BuildInfo
import prowse.domain.BuildInfoHelper._
import prowse.http.Cacheable.StaticCacheableContent
import prowse.http.PlayCacheable._
import prowse.http.StrongETag
import prowse.metrics.Instrumented

@Singleton
class TickerController extends Controller with Instrumented {

  val getTimer: Timer = metrics.timer("get")

  def get = getTimer.time {
    staticConditionalAction(
      StaticCacheableContent(
        StrongETag(BuildInfo.gitChecksum),
        buildDateTime,
        html.ticker.apply()
      )
    )
  }

}
