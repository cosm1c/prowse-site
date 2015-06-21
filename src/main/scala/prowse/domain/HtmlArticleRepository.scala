package prowse.domain

import java.time.ZonedDateTime

import play.twirl.api.Html
import prowse.http.Cacheable.{CacheableContent, StaticCacheableContent}
import prowse.http.ETag
import html.pageTemplate

import scala.concurrent.Future

// NOTE: see About Page @see http://schema.org/AboutPage
// NOTE: see Contact Page @see http://schema.org/ContactPage

/**
 * Required data for a http document served as a Blog or Article.
 *
 * Priority value could be included for sitemaps generation (@see http://www.sitemaps.org/protocol.html).
 *
 * Although eTag is preferred, it can be used for tracking and so disabled due to privacy issues by some users.
 * So both eTag and Last-Modified should be included in responses.
 *
 * Absolute limit for entire URL that also includes scheme, host, parameters etc is 1855 characters.
 *
 * @param eTag ETag for document (also @see Cacheable).
 * @param dateModified DateTime document was last modified (also @see Cacheable).
 * @param datePublished DateTime document was initially published.
 * @param title Title for html, should be between 15 and 60 characters long - including any site wide prefix/suffix.
 * @param description Description for html, should be limited to 156 characters.
 * @param author author of document - @see User
 * @param htmlContent Html content.
 *
 * @see http://schema.org/Article
 * @see http://schema.org/BlogPosting
 * @see http://schema.org/author
 */
case class HtmlArticle(eTag: ETag,
                       dateModified: ZonedDateTime,
                       datePublished: ZonedDateTime,
                       title: String,
                       description: String,
                       author: User,
                       htmlContent: String)

trait HtmlArticleRepository {
  def findByPath(path: String): Future[Option[HtmlArticle]]
}

object HtmlArticleRepository {

  def articleToCacheableHtml(article: HtmlArticle): CacheableContent[Html] =
    StaticCacheableContent(article.eTag, article.dateModified,
      pageTemplate.apply(article.title, article.description, article.author)(Html(article.htmlContent)))

}
