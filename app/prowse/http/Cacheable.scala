package prowse.http

import java.time.ZonedDateTime

import scala.annotation.implicitNotFound
import scala.language.implicitConversions

trait Cacheable {

  type LastModified = ZonedDateTime

  trait CacheableContent[C] {
    def eTag: ETag

    def lastModified: LastModified

    def content: C
  }

  case class StaticCacheableContent[C](eTag: ETag, lastModified: LastModified, content: C) extends CacheableContent[C]


  type QueryCondition = Either[Seq[ETag], LastModified]

  protected val starETagHeader = Seq(StarETag)

  @implicitNotFound("No member of type class ConditionalQuery in scope for ${Q}")
  trait ConditionalQuery[Q] {
    def precondition(query: Q): Option[QueryCondition]

    def cacheValidation(query: Q): Option[QueryCondition]
  }

  protected def preconditionCheck[C, Q: ConditionalQuery](optionalContent: Option[CacheableContent[C]])(request: Q): Boolean = {
    optionalContent match {
      case Some(content) => preconditionCheck(content)(request)
      case None => implicitly[ConditionalQuery[Q]].precondition(request) match {
        case Some(Seq(StarETag)) => false
        case _ => true
      }
    }
  }

  // date comparisons are done to second granularity as HTTP Dates only have that resolution

  protected def preconditionCheck[C, Q: ConditionalQuery](content: CacheableContent[C])(request: Q): Boolean = {
    implicitly[ConditionalQuery[Q]].precondition(request).forall({
      case Left(headerIfMatchETags) =>
        headerIfMatchETags.exists(ifMatchETag => ifMatchETag strongComparison content.eTag)
      case Right(headerIfUnmodifiedSinceDateTime) =>
        content.lastModified.toEpochSecond <= headerIfUnmodifiedSinceDateTime.toEpochSecond
    })
  }

  protected def cacheValidationCheck[C, Q: ConditionalQuery](content: CacheableContent[C])(request: Q): Boolean = {
    implicitly[ConditionalQuery[Q]].cacheValidation(request).exists({
      case Left(headerIfNoneMatchETags) =>
        headerIfNoneMatchETags.exists(ifNoneMatchETag => ifNoneMatchETag weakComparison content.eTag)
      case Right(headerIfModifiedSinceDateTime) =>
        content.lastModified.toEpochSecond <= headerIfModifiedSinceDateTime.toEpochSecond
    })
  }

  protected def preconditionCheckWhenMissing[Q: ConditionalQuery](request: Q): Boolean = {
    implicitly[ConditionalQuery[Q]].precondition(request).
      exists(_.left.exists(starETagHeader.==))
  }

}

