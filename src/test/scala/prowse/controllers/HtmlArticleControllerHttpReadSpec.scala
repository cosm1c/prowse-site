package prowse.controllers

import prowse.domain.LoremIpsumArticleRepository.loremIpsumPath
import prowse.http.{HttpReadSpecification, PlayServerRunning}

class HtmlArticleControllerHttpReadSpec extends HttpReadSpecification with PlayServerRunning {

  override val okPath: String = "article/" + loremIpsumPath
  override val missingPath: Option[String] = Some(okPath + "-missing")

}
