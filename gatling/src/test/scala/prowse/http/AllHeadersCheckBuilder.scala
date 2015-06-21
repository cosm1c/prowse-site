package prowse.http

import com.ning.http.client.FluentCaseInsensitiveStringsMap
import io.gatling.core.check.extractor._
import io.gatling.core.check.{DefaultFindCheckBuilder, Extender}
import io.gatling.core.session.ExpressionWrapper
import io.gatling.core.validation.SuccessWrapper
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.response.Response

object AllHeadersCheckBuilder {

  val AllHeadersExtractor = new Extractor[Response, FluentCaseInsensitiveStringsMap] with SingleArity {
    val name = "allHeaders"

    def apply(prepared: Response) = Some(prepared.headers).success
  }.expression

}

object AllHeaders
  extends DefaultFindCheckBuilder[HttpCheck, Response, Response, FluentCaseInsensitiveStringsMap](
    HeaderExtender,
    PassThroughResponsePreparer,
    AllHeadersCheckBuilder.AllHeadersExtractor)
