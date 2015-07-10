package prowse.seo

import java.io.{InputStream, StringReader}
import javax.xml.parsers.DocumentBuilderFactory

import org.specs2.Specification
import org.specs2.matcher.XmlMatchers
import org.xml.sax.{EntityResolver, ErrorHandler, InputSource, SAXParseException}
import play.api.Play.current
import play.api.http.HeaderNames
import play.api.libs.ws.{WS, WSResponse}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, Helpers}
import prowse.http.{HttpHelpers, PlayServerRunning}

import scala.io.Source
import scala.util.Try
import scala.xml.parsing.ConstructingParser
import scala.xml.{Elem, XML}

class SeoSpec extends Specification with DefaultAwaitTimeout with FutureAwaits with HeaderNames with HttpHelpers with XmlMatchers with PlayServerRunning {

  val CROSS_DOMAIN_POLICY_DOCTYPE = "http://www.adobe.com/xml/dtds/cross-domain-policy.dtd"

  override def is =
    s2"""
    crossdomain.xml
      exists                                      $hasCrossDomainXml
      valid DTD                                   $crossDomainXmlHasValidDtd
      permits none                                $crossDomainXmlPermitsNone
    robots.txt
      exists                                      $hasRobotsTxt
    """

  private val port: Int = Helpers.testServerPort
  private lazy val crossdomainXmlResponse: WSResponse = await(WS.url(s"http://localhost:$port/crossdomain.xml").get())
  private lazy val robotsTxtResponse: WSResponse = await(WS.url(s"http://localhost:$port/robots.txt").get())

  private def hasCrossDomainXml = {
    crossdomainXmlResponse.status === 200
  }

  private def crossDomainXmlHasValidDtd = {
    hasCrossDomainXml.orSkip

    val parser: ConstructingParser = ConstructingParser.fromSource(Source.fromString(crossdomainXmlResponse.body), preserveWS = false)
    (parser.document().dtd.externalID.systemId mustEqual "http://www.adobe.com/xml/dtds/cross-domain-policy.dtd") and
      (validateXMLFile(crossdomainXmlResponse.body, getClass.getResourceAsStream("/cross-domain-policy.dtd")) must beSuccessfulTry)
  }

  private def crossDomainXmlPermitsNone = {
    hasCrossDomainXml.orSkip

    val xml: Elem = XML.loadString(crossdomainXmlResponse.body)
    xml must beEqualToIgnoringSpace(
      <cross-domain-policy>
        <site-control permitted-cross-domain-policies="none"/>
      </cross-domain-policy>
    )
  }

  private def hasRobotsTxt = {
    robotsTxtResponse.status === 200
  }

  private def validateXMLFile(xml: String, dtd: InputStream) = {
    val domFactory = DocumentBuilderFactory.newInstance()
    domFactory.setValidating(true)

    val builder = domFactory.newDocumentBuilder()

    builder.setEntityResolver(new EntityResolver() {
      def resolveEntity(publicId: String, systemId: String) = {
        if (CROSS_DOMAIN_POLICY_DOCTYPE == systemId) {
          println(s"systemId: $systemId")
          new InputSource(dtd)
        } else {
          throw new RuntimeException(s"Invalid DTD $systemId when expecting $CROSS_DOMAIN_POLICY_DOCTYPE")
        }
      }
    })

    builder.setErrorHandler(new ErrorHandler {
      override def warning(exception: SAXParseException): Unit = throw exception

      override def error(exception: SAXParseException): Unit = throw exception

      override def fatalError(exception: SAXParseException): Unit = throw exception
    })

    Try(builder.parse(new InputSource(new StringReader(xml))))
  }

}
