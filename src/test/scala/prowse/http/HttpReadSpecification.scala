package prowse.http

import org.specs2.Specification
import org.specs2.execute.{Result, Skipped}
import play.api.Play.current
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WS, WSResponse}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, Helpers}

abstract class HttpReadSpecification extends Specification with DefaultAwaitTimeout with FutureAwaits with HeaderNames with HttpHelpers {

  protected val okPath: String
  protected val missingPath: Option[String]

  override def is = s"""Http Read Support for existing path "$okPath" and missing path "$missingPath"""" ^
    s2"""
    http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics

    http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics#section-4.1
    All general-purpose servers MUST support the methods GET and HEAD.

    http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics-26#section-4.3.1
    GET response for path that exists
      has 200 OK status                                                               $getResponseHasStatus200

      http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics#section-3.1.1.5
      has Content-Type header                                                         $getResponseHasContentTypeHeader

      http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics#section-7.1.1.1
      http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics#section-7.1.1.2
      has a DATE header in IMF-fixdate format (RFC5322)                               $getResponseHasValidDateHeader

    http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics#section-4.3.2
    HEAD response for path that exists
      has 200 Ok status                                                               $headResponseHasStatus200
      has no message body                                                             $headResponseHasNoBody
      has same response headers as equivalent GET - payload header fields optional    $headResponseHasSameHeadersAsGet

      http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics#section-3.1.1.5
      has Content-Type header                                                         $headResponseHasContentTypeHeader

      http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics#section-7.1.1.1
      http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics#section-7.1.1.2
      has a DATE header in IMF-fixdate format (RFC5322)                               $headResponseHasValidDateHeader

    GET response for path that is missing
      has 404 Not-Found status                                                        $missingGetResponseHasStatus404

      http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics#section-7.1.1.1
      http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics#section-7.1.1.2
      has a DATE header in IMF-fixdate format (RFC5322)                               $missingGetResponseHasValidDateHeader

    HEAD response for path that is missing
      has 404 Not-Found status                                                        $missingHeadResponseHasStatus404

      http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics#section-7.1.1.1
      http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics#section-7.1.1.2
      has a DATE header in IMF-fixdate format (RFC5322)                               $missingHeadResponseHasValidDateHeader
  """

  private val port: Int = Helpers.testServerPort

  private case class Response(get: WSResponse, head: WSResponse)

  private def getAndHead(url: String): Response = {
    val req = WS.url(url)
    Response(await(req.get()), await(req.head()))
  }

  private lazy val existsResponse: Response = getAndHead(s"http://localhost:$port/$okPath")
  private lazy val missingResponse: Response = getAndHead(s"http://localhost:$port/${missingPath.get}")

  private def getResponseHasStatus200: Result = {
    existsResponse.get.status mustEqual OK
  }

  private def getResponseHasValidDateHeader: Result = {
    existsResponse.get.header(DATE) must beSome.which(isValidHttpDate)
  }

  private def getResponseHasContentTypeHeader: Result = {
    existsResponse.get.header(CONTENT_TYPE) must beSome
  }

  private def headResponseHasStatus200: Result = {
    existsResponse.head.status mustEqual OK
  }

  private def headResponseHasValidDateHeader: Result = {
    existsResponse.head.header(DATE) must beSome.which(isValidHttpDate)
  }

  private def headResponseHasContentTypeHeader: Result = {
    existsResponse.head.header(CONTENT_TYPE) must beSome
  }

  private def headResponseHasNoBody: Result = {
    existsResponse.head.body mustEqual ""
  }

  private def headResponseHasSameHeadersAsGet: Result = {

    val ignoreHeaderNames = Seq(DATE)
    val optionalHeaderNames = Seq(CONTENT_LENGTH, CONTENT_RANGE, TRAILER, TRANSFER_ENCODING)

    def mandatoryHeaders(response: WSResponse) = {
      val excludeHeaderNames = optionalHeaderNames ++ ignoreHeaderNames
      response.allHeaders.filterKeys(!excludeHeaderNames.contains(_))
    }

    def optionalHeaders(response: WSResponse) = {
      val includeHeaderNames = response.allHeaders.keySet.filter(optionalHeaderNames.contains(_))
      response.allHeaders.filter(elem => includeHeaderNames.contains(elem._1))
    }

    (mandatoryHeaders(existsResponse.get) mustEqual mandatoryHeaders(existsResponse.head)) and
      (optionalHeaders(existsResponse.get) mustEqual optionalHeaders(existsResponse.head))
  }

  private def ifMissingPathProvided(block: => Result) = {
    if (missingPath.isEmpty)
      Skipped("No path provided to test 404 Not-Found")
    else
      block
  }

  private def missingGetResponseHasStatus404: Result = {
    ifMissingPathProvided {
      missingResponse.get.status mustEqual NOT_FOUND
    }
  }

  private def missingGetResponseHasValidDateHeader: Result = {
    ifMissingPathProvided {
      missingResponse.get.header(DATE) must beSome.which(isValidHttpDate)
    }
  }

  private def missingHeadResponseHasStatus404: Result = {
    ifMissingPathProvided {
      missingResponse.head.status mustEqual NOT_FOUND
    }
  }

  private def missingHeadResponseHasValidDateHeader: Result = {
    ifMissingPathProvided {
      missingResponse.head.header(DATE) must beSome.which(isValidHttpDate)
    }
  }

}
