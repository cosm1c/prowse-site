package prowse.domain

import java.time.ZonedDateTime

import play.twirl.api.Html
import prowse.http.StrongETag
import views.html.pageTemplate

object HtmlArticleStubs {

  val simpleZonedDateTime = ZonedDateTime.now()

  private val user1: User = User("username1", "firstname1", "lastname1", "email1@localhoist", "http://localhost/1")

  val simpleTextArticle = HtmlArticle(
    StrongETag("simpleETag"),
    simpleZonedDateTime,
    simpleZonedDateTime,
    "SimpleText Title",
    "SimpleText Description",
    user1,
    "SimpleText content")

  val simpleTextArticleHtml: Html = pageTemplate.render(
    simpleTextArticle.title,
    simpleTextArticle.description,
    simpleTextArticle.author.email,
    Html(simpleTextArticle.htmlContent)
  )

}
