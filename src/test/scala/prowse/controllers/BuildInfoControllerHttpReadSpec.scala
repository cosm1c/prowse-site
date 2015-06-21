package prowse.controllers

import prowse.http.{HttpReadSpecification, PlayServerRunning}

class BuildInfoControllerHttpReadSpec extends HttpReadSpecification with PlayServerRunning {

  override val okPath: String = "buildInfo"
  override val missingPath: Option[String] = None

}
