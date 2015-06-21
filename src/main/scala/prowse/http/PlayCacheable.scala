package prowse.http

import play.api.Logger
import play.api.http.HeaderNames._
import play.api.http.Writeable
import play.api.mvc.Results._
import play.api.mvc._
import prowse.ComponentRegistry._
import prowse.http.Cacheable._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object PlayCacheable {

  implicit object ConditionalQueryRequestContext extends ConditionalQuery[Request[AnyContent]] {

    override def precondition(request: Request[AnyContent]): Option[QueryCondition] = parsePrecondition(request)

    override def cacheValidation(request: Request[AnyContent]): Option[QueryCondition] = parseCacheValidation(request)

    private val parsePrecondition = parseConditionalQueryHeaders(IF_MATCH, IF_UNMODIFIED_SINCE)(_)

    private val parseCacheValidation = parseConditionalQueryHeaders(IF_NONE_MATCH, IF_MODIFIED_SINCE)(_)

    private def parseConditionalQueryHeaders(eTagHeaderName: String, lastModifiedHeaderName: String)(request: Request[AnyContent]) = {
      val headers: Headers = request.headers

      def optionalETag: Option[QueryCondition] =
        headers.get(eTagHeaderName)
          .map(ETag.parseETagsHeader)
          .map(Left(_))

      def optionalLastModified: Option[QueryCondition] =
        headers.get(lastModifiedHeaderName)
          .flatMap(parseLastModifiedHeaderValue)
          .map(Right(_))

      optionalETag.orElse(optionalLastModified)
    }

    private def parseLastModifiedHeaderValue(value: String): Option[LastModified] = timeService.parseDate(value).toOption

  }

  /**
   * This action is only recommended to be stored as a val for reuse, otherwise use normal conditionalAction.
   */
  def staticConditionalAction[C: Writeable](content: CacheableContent[C]): Action[AnyContent] = {
    Logger.logger.trace(s"Creating staticConditionalAction for content $content")

    val responseHeaders = cachingHeadersFor(content)
    val okResponse = Ok(content.content).withHeaders(responseHeaders: _*)
    val notModifiedResponse = NotModified.withHeaders(responseHeaders: _*)
    val preconditionFailedResponse = PreconditionFailed.withHeaders(responseHeaders: _*)

    val staticPreconditionCheck = preconditionCheck(content)(_: Request[AnyContent])
    val staticCacheValidationCheck = cacheValidationCheck(content)(_: Request[AnyContent])

    Action { request =>
      timeService.dateHeader {
        if (!staticPreconditionCheck(request))
          preconditionFailedResponse
        else if (staticCacheValidationCheck(request))
          notModifiedResponse
        else
          okResponse
      }
    }
  }

  def conditionalAsyncAction[C: Writeable](block: => Future[Option[CacheableContent[C]]]): Action[AnyContent] = Action.async { request =>
    block.map {
      case Some(content) => resultIsCacheableContent(request, content)
      case None => resultIsMissing(request)
    }.map(timeService.dateHeader(_))
  }

  def conditionalAction[C: Writeable](block: => Option[CacheableContent[C]]): Action[AnyContent] = Action { request =>
    timeService.dateHeader {
      block match {
        case Some(content) => resultIsCacheableContent(request, content)
        case None => resultIsMissing(request)
      }
    }
  }

  private def resultIsCacheableContent[C: Writeable](request: Request[AnyContent], content: CacheableContent[C]): Result = {
    val result: Result =
      if (!preconditionCheck(content)(request))
        PreconditionFailed
      else if (cacheValidationCheck(content)(request))
        NotModified
      else
        Ok(content.content)

    result.withHeaders(cachingHeadersFor(content): _*)
  }

  private def resultIsMissing(request: Request[AnyContent]): Result = {
    if (preconditionCheckWhenMissing(request))
      PreconditionFailed
    else
      NotFound
  }

  private def cachingHeadersFor[C](content: CacheableContent[C]) =
    Seq(
      ETAG -> content.eTag.toString,
      LAST_MODIFIED -> timeService.formatHttpDate(content.lastModified.toInstant),
      // TODO: Cache-Control header should be dynamic (allow to change with server load)
      // minimum retention of 1 second as that is resolution of HTTP dates
      CACHE_CONTROL -> "max-age=1,s-maxage=1"
    )

}
