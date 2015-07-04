package prowse.http

import com.ning.http.client.FluentCaseInsensitiveStringsMap
import io.gatling.core.Predef._
import io.gatling.http.HeaderNames._
import io.gatling.http.Predef._

import scala.collection.JavaConversions._

object HttpSemanticsAndContents extends HttpHelpers {

  val PATH_KEY = "path"

  private val GET_HEADERS_KEY = "GetRequestHeaders"
  private val ignoreHeaderNames = Seq(Date)
  private val optionalHeaderNames = Seq(ContentLanguage, ContentRange, Trailer, TransferEncoding)
  private val nonMandatoryHeaderNames = ignoreHeaderNames ++ optionalHeaderNames

  val withExistingPaths =
    group("Http Semantics and Contents - https://tools.ietf.org/html/rfc7231") {

      group("All general-purpose servers MUST support the methods GET and HEAD [section-4.1]") {

        exec(
          http("GET response for path that exists has 200 OK status, Content-Type header and a DATE header in IMF-fixdate format (RFC5322) [section-4.3.1, section-3.1.1.5, section-7.1.1.1, section-7.1.1.2]")
            .get("${path}")
            .check(
              status.is(200),
              header(ContentType).exists,
              header(Date).transform(isValidHttpDate).is(true),
              AllHeaders.saveAs(GET_HEADERS_KEY)
            )
        )
          .exec(
            http("HEAD response for path that exists, has 200 Ok status, no message body, same response headers as equivalent GET (payload header fields optional), Content-Type header and a DATE header in IMF-fixdate format (RFC5322) [section-4.3.1, section-3.1.1.5, section-7.1.1.1, section-7.1.1.2]")
              .head("${path}")
              .check(
                status.is(200),
                bodyString.is(""),
                header(ContentType).exists,
                header(Date).transform(isValidHttpDate).is(true),
                AllHeaders.transform(_.deleteAll(nonMandatoryHeaderNames))
                  .is(session => session(GET_HEADERS_KEY).as[FluentCaseInsensitiveStringsMap].deleteAll(nonMandatoryHeaderNames)),
                AllHeaders.transform(_.filterKeys(optionalHeaderNames.contains(_)))
                  .is(session => session(GET_HEADERS_KEY).as[FluentCaseInsensitiveStringsMap].filterKeys(optionalHeaderNames.contains(_)))
              )
          )
      }
    }

  val withMissingPaths =
    group("Http Read Support - https://tools.ietf.org/html/rfc7231") {
      exec(
        http("GET response for path that is missing has 404 Not-Found status and has a DATE header in IMF-fixdate format (RFC5322) [section-7.1.1.1, section-7.1.1.2]")
          .get("${path}")
          .check(
            status.is(404),
            header(Date).transform(isValidHttpDate).is(true)
          )
      )
        .exec(
          http("HEAD response for path that is missing has 404 Not-Found status and has a DATE header in IMF-fixdate format (RFC5322) [section-7.1.1.1, section-7.1.1.2]")
            .head("${path}")
            .check(
              status.is(404),
              header(Date).transform(isValidHttpDate).is(true)
            )
        )
    }

}
