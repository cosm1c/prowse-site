package prowse

import com.google.inject.AbstractModule
import prowse.domain.{HtmlArticleRepository, LoremIpsumArticleRepository}

class MainModule extends AbstractModule {
  def configure() = {

    bind(classOf[HtmlArticleRepository])
      .to(classOf[LoremIpsumArticleRepository]).asEagerSingleton()
  }
}
