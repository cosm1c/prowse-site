package prowse.domain

import java.time.{ZoneId, ZonedDateTime}

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import prowse.domain.BuildInfoHelper.buildDateTime
import prowse.domain.LoremIpsumArticleRepository._
import prowse.http.StrongETag

import scala.concurrent.Future

class LoremIpsumArticleRepository extends HtmlArticleRepository {
  override def findByPath(path: String): Future[Option[HtmlArticle]] = {
    Future.successful(
      if (path == loremIpsumPath) {
        Some(htmlArticle)
      } else {
        None
      }
    )
  }
}

object LoremIpsumArticleRepository {

  val loremIpsumPath = "loremIpsum"

  val title = "Lorem Ipsum"

  val description = "The classical Latin literature from 45 BC"

  val user = new User("loremIpsumUser", "Lorem", "Ipsum", "loremipsum@localhost", "http://localhost")

  val loremImpsumText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt" +
    " ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut" +
    " aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore" +
    " eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt" +
    " mollit anim id est laborum."

  val eTag = StrongETag(
    BuildInfo.gitChecksum +
      '!' +
      Hashing.sha1().hashString(loremImpsumText, Charsets.UTF_8).toString)

  val datePublished = ZonedDateTime.of(2015, 3, 1, 14, 30, 0, 0, ZoneId.of("GMT"))

  val htmlArticle = new HtmlArticle(
    eTag, buildDateTime, datePublished, title, description, user, loremImpsumText
  )

}
