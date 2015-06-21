package prowse.controllers

import javax.inject.Singleton

import nl.grons.metrics.scala.Timer
import play.api.libs.json.JsValue
import play.api.libs.json.Json._
import play.api.mvc.Controller
import prowse.domain.BuildInfo
import prowse.domain.BuildInfoHelper.buildDateTime
import prowse.http.Cacheable._
import prowse.http.PlayCacheable._
import prowse.http.StrongETag
import prowse.metrics.Instrumented

@Singleton
class BuildInfoController extends Controller with Instrumented {

  val jsonTimer: Timer = metrics.timer("get-json")
  val htmlTimer: Timer = metrics.timer("get-html")

  val jsonResponseContent: JsValue =
    toJson(
      Map(
        "name" -> BuildInfo.name,
        "version" -> BuildInfo.version,
        "gitChecksum" -> BuildInfo.gitChecksum,
        "buildInstant" -> BuildInfo.buildInstant
      )
    )

  val getBuildInfoJson = jsonTimer.time {
    staticConditionalAction(
      StaticCacheableContent(
        StrongETag(BuildInfo.gitChecksum),
        buildDateTime,
        jsonResponseContent
      )
    )
  }

  val getBuildInfoHtml = htmlTimer.time {
    staticConditionalAction(
      StaticCacheableContent(
        StrongETag(BuildInfo.gitChecksum),
        buildDateTime,
        html.buildInfo.apply()
      )
    )
  }

}
