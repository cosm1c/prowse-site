package prowse.controllers

import javax.inject.{Inject, Singleton}

import nl.grons.metrics.scala.Timer
import play.api.libs.json.JsValue
import play.api.libs.json.Json._
import play.api.mvc.Controller
import prowse.domain.BuildInfo
import prowse.domain.BuildInfoHelper.buildDateTime
import prowse.http.Cacheable._
import prowse.http.{CacheableAction, StrongETag}
import prowse.metrics.Instrumented
import prowse.service.TimeService

import scala.concurrent.Future

@Singleton
class BuildInfoController @Inject()(implicit val timeService: TimeService) extends Controller with Instrumented {

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

  val getBuildInfoJson =
    CacheableAction(
      jsonTimer.time(
        Future.successful(
          Some(
            StaticCacheableContent(
              StrongETag(BuildInfo.gitChecksum),
              buildDateTime,
              jsonResponseContent
            )
          )
        )
      )
    )

  val getBuildInfoHtml =
    CacheableAction(
      htmlTimer.time(
        Future.successful(
          Some(
            StaticCacheableContent(
              StrongETag(BuildInfo.gitChecksum),
              buildDateTime,
              html.buildInfo.apply()
            )
          )
        )
      )
    )

}
