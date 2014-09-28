package prowse.http

import java.time.ZonedDateTime

import org.specs2.Specification
import org.specs2.execute.{Failure, Result}
import play.api.Play.current
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WS, WSRequestHolder, WSResponse}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, Helpers}

import scala.util.{Success, Try}

abstract class HttpConditionSpecification extends Specification with DefaultAwaitTimeout with FutureAwaits with HeaderNames with HttpHelpers {

  protected val okPath: String
  protected val missingPath: Option[String]

  override def is = s2""" ${ s"""Http Conditional Support for existing path "$okPath" and missing path "$missingPath"""".title}
    http://tools.ietf.org/html/draft-ietf-httpbis-p4-conditional

    http://tools.ietf.org/html/draft-ietf-httpbis-p4-conditional#section-2.4
    http://tools.ietf.org/html/draft-ietf-httpbis-p4-conditional#section-2.2
    Has both a Strong ETag and a Last-Modified                                                              $hasMetaDataCachingHttpHeaders

    http://tools.ietf.org/html/draft-ietf-httpbis-p4-conditional#section-2.2.1
    MUST NOT send a Last-Modified date that is later than the server's time of message origination (Date)   $lastModifiedIsNotAfterDate

    http://tools.ietf.org/html/draft-ietf-httpbis-p4-conditional#section-2.3
    ETag only contains characters 0x21 and 0x23 to 0x7E                                                     $entityTagHasOnlyAllowedChars
    Avoid backslash characters in entity tags                                                               $entityTagHasNoSlash

    http://tools.ietf.org/html/draft-ietf-httpbis-p4-conditional#section-3.1
    Supports If-Match header
      If the field-value is "*", the condition is
        false when target does not currently exist                                                          $ifMatchStarIsFalseWhenNotFound
        true when target currently exists                                                                   $ifMatchStarIsTrueWhenExists
      If the field-value is a list of entity-tags, the condition is
        true if any match the current representation                                                        $ifMatchTrueWhenExists
        false if none match the current representation                                                      $ifMatchFalseWhenMissing
      Must use the strong comparison function when comparing entity-tags                                    $ifMatchUsesStrongETagComparison

    http://tools.ietf.org/html/draft-ietf-httpbis-p4-conditional#section-3.2
    Supports If-None-Match header
      GET or HEAD should respond with 304 (Not Modified) when one of the stored responses matches           $ifNoneMatchRespondsNotModifiedWhenExists
      Must use the weak comparison function when comparing entity-tags                                      $ifNoneMatchUsesWeakETagComparison
      If the field-value is "*", the condition is false if the origin server has a current representation   $ifNoneMatchStarIsFalseWhenExists

    http://tools.ietf.org/html/draft-ietf-httpbis-p4-conditional#section-3.3
    Supports If-Modified-Since header
      Must ignore If-Modified-Since if the request contains an If-None-Match header field                   $ifModifiedSinceIgnoredWhenIfNoneMatch
      Must ignore the If-Modified-Since header field if the received field-value is not a valid HTTP-date   $ifModifiedSinceIgnoresInvalidDates
      Should not perform the action if the last modification date is <= to the date provided                $ifModifiedSinceFailsWhenDateNotAfter

    http://tools.ietf.org/html/draft-ietf-httpbis-p4-conditional#section-3.4
    Support If-Unmodified-Since
      Must ignore If-Unmodified-Since if the request contains an If-Match header field                      $ifUnmodifiedSinceIgnoredWhenIfMatchPresent
      Must ignore the If-Unmodified-Since header field if the field-value is not a valid HTTP-date          $ifUnmodifiedSinceIgnoredIfInvalidDate
      Must not perform the action when the last modification date is more recent than the date provided     $ifUnmodifiedSinceFailsWhenBeforeLastModified

    http://tools.ietf.org/html/draft-ietf-httpbis-p4-conditional#section-4.1
    Support Not-Modified (304) response
      304 response cannot contain a message-body                                                            $notModifiedResponseHasNoBody
      304 response has listed header fields sent in a 200 (OK) response to the same request                 $notModifiedHeadersMatchOkResponse
      Should not generate metadata other than listed fields unless for guiding cache updates                $notModifiedHeadersWithinAllowedList

    http://tools.ietf.org/html/draft-ietf-httpbis-p4-conditional#section-6
    Precedence
      Honors If-Match when it fails with 412                                                                $precedenceIfMatch
      Ignores If-Unmodified-Since when If-Match present                                                     $precedenceIfMatchFirst
      Ignores If-Unmodified-Since when Invalid If-Match present                                             $precedenceIfMatchWhenInvalid
      Honors If-None-Match when it fails with 304 for GET/HEAD                                              $precedenceIfNoneMatch
      Ignores If-Modified-Since when If-Non-Match present                                                   $precedenceIfNoneMatchWhenIfModifiedSince
      Ignores If-Modified-Since when Invalid If-Non-Match present                                           $precedenceInvalidIfNoneMatchWhenIfModifiedSince

    http://tools.ietf.org/html/draft-ietf-httpbis-p6-cache#section-4.2.1
    Cache freshness
      Must have one Cache-Control header                                                                    $cacheControlHeaderPresent
      Must not use Expires header (in preference for Cache-Control header)                                  $legacyExpiresHeaderIsNotUsed
      Cache-Control must have one max-age                                                                   $cacheControlHasOneMaxAge
      Cache-Control must not have multiple values for any directive                                         $cacheControlHasSingleEntriesOnly

      http://tools.ietf.org/html/draft-ietf-httpbis-p6-cache#section-5.2.2.8
      max-age is a single numeric                                                                           $cacheControlMaxAgeIsSingleNumeric

      http://tools.ietf.org/html/draft-ietf-httpbis-p6-cache#section-5.2.2.8
      smax-age is a single numeric                                                                          $cacheControlSMaxAgeIsNumeric
  """

  private val port: Int = Helpers.testServerPort
  private lazy val request: WSRequestHolder = WS.url(s"http://localhost:$port/$okPath")

  // TODO: Cleanup this repetitive (non-DRY), unmaintainable, unreadable, ugly code (see: HttpReadSpecification TODO)

  private lazy val responseFromGet = await(request.get())
  private lazy val responseFromGetIfMatchStar = await(request.withHeaders(IF_MATCH -> "*").get())
  private lazy val responseFromGetIfMatchTrue = await(request.withHeaders(IF_MATCH -> s""""ETag1", ${responseFromGet.header(ETAG).get}, "ETag2"""").get())
  private lazy val responseFromGetIfMatchFalse = await(request.withHeaders(IF_MATCH -> s""""ETag1", "ETag2"""").get())
  private lazy val responseFromGetIfMatchWeakETag = await(request.withHeaders(IF_MATCH -> s"""W/${responseFromGet.header(ETAG).get}""").get())
  private lazy val responseFromGetIfNoneMatchTrue = await(request.withHeaders(IF_NONE_MATCH -> responseFromGet.header(ETAG).get).get())
  private lazy val responseFromGetIfNoneMatchWeak = await(request.withHeaders(IF_NONE_MATCH -> s"""W/${responseFromGet.header(ETAG).get}""").get())
  private lazy val responseFromGetIfNoneMatchStar = await(request.withHeaders(IF_NONE_MATCH -> "*").get())
  private lazy val responseFromGetIfModifiedSinceWhenIfNoneMatch = await(request.withHeaders(IF_NONE_MATCH -> "\"Non-matching ETag\"", IF_MODIFIED_SINCE -> responseFromGet.header(LAST_MODIFIED).get).get())
  private lazy val responseFromGetIfModifiedSinceWhenInvalidDate = await(request.withHeaders(IF_MODIFIED_SINCE -> "Invalid HTTP Date").get())
  private lazy val responseFromGetWhenIfModifiedSinceIsEqual = await(request.withHeaders(IF_MODIFIED_SINCE -> responseFromGet.header(LAST_MODIFIED).get).get())
  private lazy val responseFromGetWhenIfModifiedSinceIsBefore = await(request.withHeaders(IF_MODIFIED_SINCE -> printHttpDateString(lastModifiedHeaderFromGet.minusDays(1))).get())
  private lazy val responseFromGetWhenIfModifiedSinceIsAfter = await(request.withHeaders(IF_MODIFIED_SINCE -> printHttpDateString(lastModifiedHeaderFromGet.plusDays(1))).get())
  private lazy val responseFromGetIfUnmodifiedSinceWithBadETag = await(request.withHeaders(IF_UNMODIFIED_SINCE -> responseFromGet.header(LAST_MODIFIED).get, IF_MATCH -> "\"Non-matching ETag\"").get())
  private lazy val responseFromGetIfUnmodifiedSinceWithInvalidDate = await(request.withHeaders(IF_UNMODIFIED_SINCE -> "Invalid Date").get())
  private lazy val responseFromGetWhenIfUnmodifiedSinceIsEqual = await(request.withHeaders(IF_UNMODIFIED_SINCE -> responseFromGet.header(LAST_MODIFIED).get).get())
  private lazy val responseFromGetWhenIfUnmodifiedSinceIsBefore = await(request.withHeaders(IF_UNMODIFIED_SINCE -> printHttpDateString(lastModifiedHeaderFromGet.minusDays(1))).get())
  private lazy val responseFromGetWhenIfUnmodifiedSinceIsAfter = await(request.withHeaders(IF_UNMODIFIED_SINCE -> printHttpDateString(lastModifiedHeaderFromGet.plusDays(1))).get())
  private lazy val responseFromGetWhenOnlyIfMatchFalse = await(request.withHeaders(IF_MATCH -> "\"NOT-EQUAL\"", IF_UNMODIFIED_SINCE -> responseFromGet.header(LAST_MODIFIED).get, IF_NONE_MATCH -> responseFromGet.header(ETAG).get, IF_MODIFIED_SINCE -> responseFromGet.header(LAST_MODIFIED).get).get())
  private lazy val responseFromGetWhenLastModifedAndIfMatchInvalid = await(request.withHeaders(IF_MATCH -> "\"INVALID ETAG", IF_UNMODIFIED_SINCE -> responseFromGet.header(LAST_MODIFIED).get, IF_NONE_MATCH -> responseFromGet.header(ETAG).get, IF_MODIFIED_SINCE -> responseFromGet.header(LAST_MODIFIED).get).get())
  private lazy val responseFromGetIfModifiedSinceWhenInvalidIfNoneMatch = await(request.withHeaders(IF_NONE_MATCH -> "\"Invalid ETag", IF_MODIFIED_SINCE -> responseFromGet.header(LAST_MODIFIED).get).get())

  private lazy val responseFromHead = await(request.head())
  private lazy val responseFromHeadIfMatchStar = await(request.withHeaders(IF_MATCH -> "*").head())
  private lazy val responseFromHeadIfMatchTrue = await(request.withHeaders(IF_MATCH -> s""""ETag1", ${responseFromHead.header(ETAG).get}, "ETag2"""").head())
  private lazy val responseFromHeadIfMatchFalse = await(request.withHeaders(IF_MATCH -> s""""ETag1", "ETag2"""").head())
  private lazy val responseFromHeadIfMatchWeakETag = await(request.withHeaders(IF_MATCH -> s"""W/${responseFromHead.header(ETAG).get}""").head())
  private lazy val responseFromHeadIfNoneMatchTrue = await(request.withHeaders(IF_NONE_MATCH -> responseFromHead.header(ETAG).get).head())
  private lazy val responseFromHeadIfNoneMatchWeak = await(request.withHeaders(IF_NONE_MATCH -> s"""W/${responseFromHead.header(ETAG).get}""").head())
  private lazy val responseFromHeadIfNoneMatchStar = await(request.withHeaders(IF_NONE_MATCH -> "*").head())
  private lazy val responseFromHeadIfModifiedSinceWhenIfNoneMatch = await(request.withHeaders(IF_NONE_MATCH -> "\"Non-matching ETag\"", IF_MODIFIED_SINCE -> responseFromHead.header(LAST_MODIFIED).get).head())
  private lazy val responseFromHeadIfModifiedSinceWhenInvalidDate = await(request.withHeaders(IF_MODIFIED_SINCE -> "Invalid HTTP Date").get())
  private lazy val responseFromHeadWhenIfModifiedSinceIsEqual = await(request.withHeaders(IF_MODIFIED_SINCE -> responseFromHead.header(LAST_MODIFIED).get).get())
  private lazy val responseFromHeadWhenIfModifiedSinceIsBefore = await(request.withHeaders(IF_MODIFIED_SINCE -> printHttpDateString(lastModifiedHeaderFromHead.minusDays(1))).head())
  private lazy val responseFromHeadWhenIfModifiedSinceIsAfter = await(request.withHeaders(IF_MODIFIED_SINCE -> printHttpDateString(lastModifiedHeaderFromHead.plusDays(1))).head())
  private lazy val responseFromHeadIfUnmodifiedSinceWithBadETag = await(request.withHeaders(IF_UNMODIFIED_SINCE -> responseFromHead.header(LAST_MODIFIED).get, IF_MATCH -> "\"Non-matching ETag\"").head())
  private lazy val responseFromHeadIfUnmodifiedSinceWithInvalidDate = await(request.withHeaders(IF_UNMODIFIED_SINCE -> "Invalid Date").head())
  private lazy val responseFromHeadWhenIfUnmodifiedSinceIsEqual = await(request.withHeaders(IF_UNMODIFIED_SINCE -> responseFromHead.header(LAST_MODIFIED).get).head())
  private lazy val responseFromHeadWhenIfUnmodifiedSinceIsBefore = await(request.withHeaders(IF_UNMODIFIED_SINCE -> printHttpDateString(lastModifiedHeaderFromHead.minusDays(1))).head())
  private lazy val responseFromHeadWhenIfUnmodifiedSinceIsAfter = await(request.withHeaders(IF_UNMODIFIED_SINCE -> printHttpDateString(lastModifiedHeaderFromHead.plusDays(1))).head())
  private lazy val responseFromHeadWhenOnlyIfMatchFalse = await(request.withHeaders(IF_MATCH -> "\"NOT-EQUAL\"", IF_UNMODIFIED_SINCE -> responseFromHead.header(LAST_MODIFIED).get, IF_NONE_MATCH -> responseFromHead.header(ETAG).get, IF_MODIFIED_SINCE -> responseFromHead.header(LAST_MODIFIED).get).head())
  private lazy val responseFromHeadWhenLastModifedAndIfMatchInvalid = await(request.withHeaders(IF_MATCH -> "\"INVALID ETAG", IF_UNMODIFIED_SINCE -> responseFromGet.header(LAST_MODIFIED).get, IF_NONE_MATCH -> responseFromGet.header(ETAG).get, IF_MODIFIED_SINCE -> responseFromGet.header(LAST_MODIFIED).get).head())
  private lazy val responseFromHeadIfModifiedSinceWhenInvalidIfNoneMatch = await(request.withHeaders(IF_NONE_MATCH -> "\"Invalid ETag", IF_MODIFIED_SINCE -> responseFromGet.header(LAST_MODIFIED).get).get())

  private lazy val responseFromMissingGetIfMatchStar = await(WS.url(s"http://localhost:$port/${missingPath.get}").withHeaders(IF_MATCH -> "*").get())
  private lazy val responseFromMissingHeadIfMatchStar = await(WS.url(s"http://localhost:$port/${missingPath.get}").withHeaders(IF_MATCH -> "*").head())

  private def hasMetaDataCachingHttpHeaders = {
    hasETagHeader and
      hasLastModifiedHeader and
      hasSameValueForETag and
      hasSameValueForLastModified and
      hasStrongETag
  }

  private def lastModifiedIsNotAfterDate = {
    (hasDateHeader and hasLastModifiedHeader).orSkip

    !(lastModifiedHeaderFromGet isAfter dateHeaderFromGet) and
      !(lastModifiedHeaderFromHead isAfter dateHeaderFromHead)
  }

  private def entityTagHasOnlyAllowedChars = {
    hasETagHeader.orSkip

    val allowedETagChars: Seq[Char] = (Seq(0x21) ++ Range(0x23, 0x7E).toSeq).map(_.toChar)

    (etagHeaderFromGet.filterNot(allowedETagChars.contains) === "\"\"") and
      (etagHeaderFromHead.filterNot(allowedETagChars.contains) === "\"\"")
  }

  private def entityTagHasNoSlash = {
    hasETagHeader.orSkip

    def hasNoSlash(seq: String) = !seq.contains('\\')

    hasNoSlash(etagHeaderFromGet) and
      hasNoSlash(etagHeaderFromHead)
  }

  private def ifMatchStarIsFalseWhenNotFound = {
    (missingPath must beSome and hasETagHeader).orSkip

    (responseFromMissingGetIfMatchStar.status mustEqual PRECONDITION_FAILED) and
      (responseFromMissingHeadIfMatchStar.status mustEqual PRECONDITION_FAILED)
  }

  private def ifMatchStarIsTrueWhenExists = {
    hasETagHeader.orSkip

    (responseFromGetIfMatchStar.status mustEqual OK) and
      (responseFromHeadIfMatchStar.status mustEqual OK)
  }

  private def ifMatchTrueWhenExists = {
    hasETagHeader.orSkip

    (responseFromGetIfMatchTrue.status mustEqual OK) and
      (responseFromHeadIfMatchTrue.status mustEqual OK)
  }

  private def ifMatchFalseWhenMissing = {
    hasETagHeader.orSkip

    (responseFromGetIfMatchFalse.status mustEqual PRECONDITION_FAILED) and
      (responseFromHeadIfMatchFalse.status mustEqual PRECONDITION_FAILED)
  }

  private def ifMatchUsesStrongETagComparison = {
    hasETagHeader.orSkip

    // Since we enforce only Strong ETags, prepend Strong ETag string with Weak prefix and ensure it fails
    (responseFromGetIfMatchWeakETag.status mustEqual PRECONDITION_FAILED) and
      (responseFromHeadIfMatchWeakETag.status mustEqual PRECONDITION_FAILED)
  }

  private def ifNoneMatchRespondsNotModifiedWhenExists = {
    hasETagHeader.orSkip

    (responseFromGetIfNoneMatchTrue.status mustEqual NOT_MODIFIED) and
      (responseFromHeadIfNoneMatchTrue.status mustEqual NOT_MODIFIED)
  }

  private def ifNoneMatchUsesWeakETagComparison = {
    hasETagHeader.orSkip

    // Since we enforce only Strong ETags, prepend Strong ETag string with Weak prefix and ensure it succeeds
    (responseFromGetIfNoneMatchWeak.status mustEqual NOT_MODIFIED) and
      (responseFromHeadIfNoneMatchWeak.status mustEqual NOT_MODIFIED)
  }

  private def ifNoneMatchStarIsFalseWhenExists = {
    hasETagHeader.orSkip

    (responseFromGetIfNoneMatchStar.status mustEqual NOT_MODIFIED) and
      (responseFromHeadIfNoneMatchStar.status mustEqual NOT_MODIFIED)
  }

  private def ifModifiedSinceIgnoredWhenIfNoneMatch = {
    hasLastModifiedHeader.orSkip

    (responseFromGetIfModifiedSinceWhenIfNoneMatch.status mustEqual OK) and
      (responseFromHeadIfModifiedSinceWhenIfNoneMatch.status mustEqual OK)
  }

  private def ifModifiedSinceIgnoresInvalidDates = {
    hasLastModifiedHeader.orSkip

    (responseFromGetIfModifiedSinceWhenInvalidDate.status mustEqual OK) and
      (responseFromHeadIfModifiedSinceWhenInvalidDate.status mustEqual OK)
  }

  private def ifModifiedSinceFailsWhenDateNotAfter = {
    hasLastModifiedHeader.orSkip

    // These sentences work with Specs2 negation
    "It should be unmodified when If-Modified-Since equal to Last-Modified" ==> ((responseFromGetWhenIfModifiedSinceIsEqual.status mustEqual NOT_MODIFIED) and (responseFromHeadWhenIfModifiedSinceIsEqual.status mustEqual NOT_MODIFIED)) and
      "It is unmodified when If-Modified-Since earlier than Last-Modified" ==> ((responseFromGetWhenIfModifiedSinceIsBefore.status mustEqual OK) and (responseFromHeadWhenIfModifiedSinceIsBefore.status mustEqual OK)) and
      "It is ok when If-Modified-Since later than Last-Modified" ==> ((responseFromGetWhenIfModifiedSinceIsAfter.status mustEqual NOT_MODIFIED) and (responseFromHeadWhenIfModifiedSinceIsAfter.status mustEqual NOT_MODIFIED))
  }

  private def ifUnmodifiedSinceIgnoredWhenIfMatchPresent = {
    hasLastModifiedHeader.orSkip

    // Ignore matching If-Unmodified-Since due to non-matching If-Match
    (responseFromGetIfUnmodifiedSinceWithBadETag.status mustEqual PRECONDITION_FAILED) and
      (responseFromHeadIfUnmodifiedSinceWithBadETag.status mustEqual PRECONDITION_FAILED)
  }

  private def ifUnmodifiedSinceIgnoredIfInvalidDate = {
    hasLastModifiedHeader.orSkip

    (responseFromGetIfUnmodifiedSinceWithInvalidDate.status mustEqual OK) and
      (responseFromHeadIfUnmodifiedSinceWithInvalidDate.status mustEqual OK)
  }

  private def ifUnmodifiedSinceFailsWhenBeforeLastModified = {
    hasLastModifiedHeader.orSkip

    // These sentences work with Specs2 negation
    "It should be ok when If-Unmodified-Since equal to Last-Modified" ==> ((responseFromGetWhenIfUnmodifiedSinceIsEqual.status mustEqual OK) and (responseFromHeadWhenIfUnmodifiedSinceIsEqual.status mustEqual OK)) and
      "It should be precondition-failed when If-Unmodified-Since earlier than Last-Modified" ==> ((responseFromGetWhenIfUnmodifiedSinceIsBefore.status mustEqual PRECONDITION_FAILED) and (responseFromHeadWhenIfUnmodifiedSinceIsBefore.status mustEqual PRECONDITION_FAILED)) and
      "It should be ok when If-Unmodified-Since later than Last-Modified" ==> ((responseFromGetWhenIfUnmodifiedSinceIsAfter.status mustEqual OK) and (responseFromHeadWhenIfUnmodifiedSinceIsAfter.status mustEqual OK))
  }

  private def notModifiedResponseHasNoBody = {
    (hasETagHeader and hasLastModifiedHeader).orSkip

    (responseFromGetIfNoneMatchTrue.status mustEqual NOT_MODIFIED) and
      (responseFromHeadIfNoneMatchTrue.status mustEqual NOT_MODIFIED) and
      (responseFromGetIfNoneMatchTrue.body.isEmpty must beTrue) and
      (responseFromHeadIfNoneMatchTrue.body.isEmpty must beTrue)
  }

  private def notModifiedHeadersMatchOkResponse = {
    (hasETagHeader and hasLastModifiedHeader).orSkip

    val headersToCheck = Seq(CACHE_CONTROL, CONTENT_LOCATION, DATE, ETAG, EXPIRES, VARY)

    def mandatoryHeadersFrom(wsResponse: WSResponse) = wsResponse.allHeaders.filter(headersToCheck.contains(_))

    (responseFromGetIfNoneMatchTrue.status mustEqual NOT_MODIFIED) and
      (responseFromHeadIfNoneMatchTrue.status mustEqual NOT_MODIFIED) and
      (mandatoryHeadersFrom(responseFromGet) mustEqual mandatoryHeadersFrom(responseFromGetIfNoneMatchTrue)) and
      (mandatoryHeadersFrom(responseFromHead) mustEqual mandatoryHeadersFrom(responseFromHeadIfNoneMatchTrue))
  }

  private def notModifiedHeadersWithinAllowedList: Result = {
    (hasETagHeader and hasLastModifiedHeader).orSkip

    // NOTE: may need to add other headers which may be valid for cache, such as Last-Modified when no eTag
    val allowedHeaders = Seq(CACHE_CONTROL, CONTENT_LOCATION, DATE, ETAG, EXPIRES, VARY, LAST_MODIFIED)
    val toleratedHeaders = Map(CONTENT_LENGTH -> Seq("0"))

    def allowedHeadersFrom(wsResponse: WSResponse): Map[String, Seq[String]] = wsResponse.allHeaders.filterKeys(!allowedHeaders.contains(_))

    val nonGetRepresentationMetadata: Map[String, Seq[String]] = allowedHeadersFrom(responseFromGetIfNoneMatchTrue)
    val nonHeadRepresentationMetadata: Map[String, Seq[String]] = allowedHeadersFrom(responseFromHeadIfNoneMatchTrue)

    if ((responseFromGetIfNoneMatchTrue.status == NOT_MODIFIED) && (responseFromHeadIfNoneMatchTrue.status == NOT_MODIFIED) &&
      ((nonGetRepresentationMetadata.toMap == toleratedHeaders) || (nonHeadRepresentationMetadata.toMap == toleratedHeaders))) {
      skipped( """NOT_MODIFIED responses should not contain a "Content-Length" but we can tolerate a value of "0"""")

    } else {
      (responseFromGetIfNoneMatchTrue.status mustEqual NOT_MODIFIED) and
        (responseFromHeadIfNoneMatchTrue.status mustEqual NOT_MODIFIED) and
        (nonGetRepresentationMetadata must be).empty and
        (nonHeadRepresentationMetadata must be).empty
    }
  }

  private def precedenceIfMatch = ifMatchFalseWhenMissing

  private def precedenceIfMatchFirst = {
    (hasETagHeader and hasLastModifiedHeader).orSkip

    (responseFromGetWhenLastModifedAndIfMatchInvalid.status === PRECONDITION_FAILED) and
      (responseFromGetWhenLastModifedAndIfMatchInvalid.status === PRECONDITION_FAILED)
  }

  private def precedenceIfMatchWhenInvalid = {
    (hasETagHeader and hasLastModifiedHeader).orSkip

    (responseFromGetWhenOnlyIfMatchFalse.status === PRECONDITION_FAILED) and
      (responseFromHeadWhenOnlyIfMatchFalse.status === PRECONDITION_FAILED)
  }

  private def precedenceIfNoneMatch = ifNoneMatchRespondsNotModifiedWhenExists

  private def precedenceIfNoneMatchWhenIfModifiedSince = ifModifiedSinceIgnoredWhenIfNoneMatch

  private def precedenceInvalidIfNoneMatchWhenIfModifiedSince = {
    hasLastModifiedHeader.orSkip

    (responseFromGetIfModifiedSinceWhenInvalidIfNoneMatch.status mustEqual OK) and
      (responseFromGetIfModifiedSinceWhenInvalidIfNoneMatch.status mustEqual OK)
  }


  private def cacheControlHeaderPresent = {
    (responseFromGet.allHeaders must haveKey(CACHE_CONTROL)) and
      (responseFromHead.allHeaders must haveKey(CACHE_CONTROL)) and
      (responseFromGet.allHeaders(CACHE_CONTROL).length === 1) and
      (responseFromHead.allHeaders(CACHE_CONTROL).length === 1)
  }

  private def legacyExpiresHeaderIsNotUsed = {
    (responseFromGet.header(EXPIRES) must beNone) and
      (responseFromHead.header(EXPIRES) must beNone)
  }

  private def cacheControlHasOneMaxAge = {
    hasCacheControlHeader.orSkip

    def countMaxAge(response: WSResponse): Int =
      groupDirectives(response.header(CACHE_CONTROL).get)("max-age").length

    (responseFromGet.header(CACHE_CONTROL) must beSome) and
      (responseFromHead.header(CACHE_CONTROL) must beSome) and
      (countMaxAge(responseFromGet) === 1) and
      (countMaxAge(responseFromHead) === 1)
  }

  private def cacheControlHasSingleEntriesOnly = {
    hasCacheControlHeader.orSkip

    def countHeaderDirectives(response: WSResponse): Iterable[Int] =
      groupDirectives(response.header(CACHE_CONTROL).get).map(_._2).map(_.length)

    (responseFromGet.header(CACHE_CONTROL) must beSome) and
      (responseFromHead.header(CACHE_CONTROL) must beSome) and
      (countHeaderDirectives(responseFromGet) must contain(1).forall) and
      (countHeaderDirectives(responseFromHead) must contain(1).forall)
  }

  private def cacheControlMaxAgeIsSingleNumeric = {
    hasCacheControlHeader.orSkip

    def maxAgeValues(response: WSResponse): Array[String] =
      groupDirectives(response.header(CACHE_CONTROL).get)("max-age")

    def maxAgeIsSingleInt(response: WSResponse): Boolean =
      maxAgeValues(response) forall (_ forall Character.isDigit)

    maxAgeIsSingleInt(responseFromGet) and
      maxAgeIsSingleInt(responseFromHead)
  }

  private def cacheControlSMaxAgeIsNumeric = {
    hasCacheControlHeader.orSkip

    def sMaxageValues(response: WSResponse): Array[String] =
      groupDirectives(response.header(CACHE_CONTROL).get)("s-maxage")

    def sMaxageIsSingleInt(response: WSResponse): Boolean =
      sMaxageValues(response) forall (_ forall Character.isDigit)

    sMaxageIsSingleInt(responseFromGet) and
      sMaxageIsSingleInt(responseFromHead)
  }

  /*
   * Helper methods
   */

  private def hasDateHeader =
    (responseFromGet.allHeaders must haveKey(DATE)) and
      (responseFromHead.allHeaders must haveKey(DATE))

  private def hasLastModifiedHeader =
    (responseFromGet.allHeaders must haveKey(LAST_MODIFIED)) and
      (responseFromHead.allHeaders must haveKey(LAST_MODIFIED))

  private def hasETagHeader =
    (responseFromGet.allHeaders must haveKey(ETAG)) and
      (responseFromHead.allHeaders must haveKey(ETAG))

  private def hasCacheControlHeader =
    (responseFromGet.allHeaders must haveKey(CACHE_CONTROL)) and
      (responseFromHead.allHeaders must haveKey(CACHE_CONTROL))

  private def hasSameValueForETag =
    etagHeaderFromGet === etagHeaderFromHead

  private def hasSameValueForLastModified =
    lastModifiedHeaderFromGet === lastModifiedHeaderFromHead

  private def hasStrongETag =
    ETag.parse(responseFromGet.header(ETAG).get) match {
      case Success(StrongETag(_, _)) => success
      case _ => Failure("Not a Strong ETag")
    }

  private def dateHeaderFromGet: ZonedDateTime =
    readDateHeader(responseFromGet, DATE, "initial GET request")

  private def dateHeaderFromHead: ZonedDateTime =
    readDateHeader(responseFromHead, DATE, "initial HEAD request")

  private def lastModifiedHeaderFromGet: ZonedDateTime =
    readDateHeader(responseFromGet, LAST_MODIFIED, "initial GET request")

  private def lastModifiedHeaderFromHead: ZonedDateTime =
    readDateHeader(responseFromHead, LAST_MODIFIED, "initial HEAD request")

  private def readDateHeader(response: WSResponse, headerName: String, requestDescription: String): ZonedDateTime = {
    val header: String = headerValue(response, headerName, requestDescription)

    val maybeTime: Try[ZonedDateTime] = parseHttpDateString(header)
    (maybeTime must beSuccessfulTry).setMessage(s"""Invalid Http DateTime "$header" in $requestDescription - ${maybeTime}""").orThrow

    maybeTime.get
  }

  private def etagHeaderFromGet: String = headerValue(responseFromGet, ETAG, "initial GET request")

  private def etagHeaderFromHead: String = headerValue(responseFromHead, ETAG, "initial GET request")

  private def headerValue(response: WSResponse, headerName: String, requestDescription: String): String = {
    val maybeHeader: Option[String] = response.header(headerName)
    (maybeHeader must beSome).setMessage(s"No $headerName header present in $requestDescription").orThrow
    maybeHeader.get
  }

  private def groupDirectives(value: String) =
    value
      .split(',')
      .map(_.trim)
      .groupBy(
        _.split('=').head
      )
      .mapValues(
        _.map(value => value.drop(value.indexOf('=') + 1))
      )

}
