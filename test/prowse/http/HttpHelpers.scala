package prowse.http

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import java.util.Locale

import scala.util.Try

trait HttpHelpers {

  private val httpDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH)
      .withZone(ZoneId.of("GMT"))

  def isValidHttpDate(s: String) = Try(httpDateFormatter.parse(s)).isSuccess

  def parseHttpDateString(s: String): Try[ZonedDateTime] = Try(ZonedDateTime.parse(s, httpDateFormatter))

  def printHttpDateString(d: ZonedDateTime): String = httpDateFormatter.format(d)

}
