package prowse

import play.api.inject.Module
import play.api.{Configuration, Environment}
import prowse.domain.{HtmlArticleRepository, LoremIpsumArticleRepository}
import prowse.service.{SystemUTCClockTimeServiceComponent, TimeService}

class MainModule extends Module {
  def bindings(env: Environment, conf: Configuration) = Seq(
    bind[TimeService].to[SystemUTCClockTimeServiceComponent],
    bind[HtmlArticleRepository].to[LoremIpsumArticleRepository]
  )
}
