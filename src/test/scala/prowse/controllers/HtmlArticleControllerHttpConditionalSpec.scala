package prowse.controllers

import prowse.domain.LoremIpsumArticleRepository.loremIpsumPath
import prowse.http.{HttpConditionSpecification, PlayServerRunning}

class HtmlArticleControllerHttpConditionalSpec extends HttpConditionSpecification with PlayServerRunning {

  override val okPath: String = "article/" + loremIpsumPath
  override val missingPath: Option[String] = Some(okPath + "-missing")

}
