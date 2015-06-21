package prowse.controllers

import prowse.http.{HttpConditionSpecification, PlayServerRunning}

class BuildInfoControllerHttpConditionalSpec extends HttpConditionSpecification with PlayServerRunning {

  override val okPath: String = "buildInfo"
  override val missingPath: Option[String] = None

}
