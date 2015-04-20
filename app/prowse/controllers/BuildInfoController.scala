package prowse.controllers

import play.api.libs.json.JsValue
import play.api.libs.json.Json._
import play.api.mvc.Controller
import prowse.domain.BuildInfo
import prowse.domain.BuildInfoHelper.buildDateTime
import prowse.http.Cacheable._
import prowse.http.PlayCacheable._
import prowse.http.StrongETag

object BuildInfoController extends Controller {

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
