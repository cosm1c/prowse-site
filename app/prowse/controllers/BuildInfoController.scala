package prowse.controllers

import buildinfo.BuildInfo
import play.api.libs.json.JsValue
import play.api.libs.json.Json._
import play.api.mvc.{Action, Controller}

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

  val getBuildInfoJson = Action {
    Ok(jsonResponseContent)
  }

}
