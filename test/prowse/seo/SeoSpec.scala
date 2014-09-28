package prowse.seo

import org.specs2.Specification
import org.specs2.matcher.XmlMatchers
import org.specs2.specification.Fragments
import play.api.Play.current
import play.api.http.HeaderNames
import play.api.libs.ws.{WS, WSResponse}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, Helpers}
import prowse.http.{HttpHelpers, PlayServerRunning}

import scala.io.Source
import scala.xml.parsing.ConstructingParser
import scala.xml.{Elem, XML}

class SeoSpec extends Specification with DefaultAwaitTimeout with FutureAwaits with HeaderNames with HttpHelpers with XmlMatchers with PlayServerRunning {
  override def is: Fragments =
    s2"""
    crossdomain.xml
      exists                                      $hasCrossDomainXml
      valid DTD                                   $crossDomainXmlHasDtd
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

  private def crossDomainXmlHasDtd = {
    hasCrossDomainXml.orSkip

    val parser: ConstructingParser = ConstructingParser.fromSource(Source.fromString(crossdomainXmlResponse.body), preserveWS = false)
    parser.document().dtd.externalID.systemId mustEqual "http://www.adobe.com/xml/dtds/cross-domain-policy.dtd"
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

}
