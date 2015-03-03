import com.google.inject.{AbstractModule, Guice}
import play.api.GlobalSettings
import prowse.domain.{HtmlArticleRepository, LoremIpsumArticleRepository}

object Global extends GlobalSettings {

  val injector = Guice.createInjector(new AbstractModule {
    protected def configure() {
      bind(classOf[HtmlArticleRepository]).to(classOf[LoremIpsumArticleRepository])
    }
  })

  override def getControllerInstance[A](controllerClass: Class[A]): A = injector.getInstance(controllerClass)

}
