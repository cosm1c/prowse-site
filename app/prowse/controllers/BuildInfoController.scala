package prowse.controllers

import java.time.{Instant, ZoneId, ZonedDateTime}

import buildinfo.BuildInfo
import play.api.libs.json.JsValue
import play.api.libs.json.Json._
import play.api.mvc.Controller
import prowse.http.{PlayCacheable, StrongETag}

object BuildInfoController extends Controller with PlayCacheable {

  private val buildDateTime: ZonedDateTime = Instant.parse(BuildInfo.buildInstant).atZone(ZoneId.of("GMT"))

  val jsonResponseContent: JsValue =
    toJson(
      Map(
        "name" -> BuildInfo.name,
        "version" -> BuildInfo.version,
        "gitChecksum" -> BuildInfo.gitChecksum,
        "buildInstant" -> BuildInfo.buildInstant
      )
    )

  val getBuildInfoJson = staticConditionalAction(
    StaticCacheableContent(
      StrongETag(BuildInfo.gitChecksum),
      buildDateTime,
      jsonResponseContent
    )
  )

  val getBuildInfoHtml = staticConditionalAction(
    StaticCacheableContent(
      StrongETag(BuildInfo.gitChecksum),
      buildDateTime,
      views.html.buildInfo.apply()
    )
  )

}
