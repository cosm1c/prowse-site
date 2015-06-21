package prowse.http

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.util.Locale

import scala.util.Try

trait HttpHelpers {

  private val gmtZoneId: ZoneId = ZoneId.of("GMT")

  private val httpDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH)
      .withZone(gmtZoneId)

  def isValidHttpDate(s: String) = Try(httpDateFormatter.parse(s)).isSuccess

  // TODO: Remove atZone and switch to ZonedDateTime.parse method when fixed on Linux
  def parseHttpDateString(s: String): Try[ZonedDateTime] = Try(LocalDateTime.parse(s, httpDateFormatter).atZone(gmtZoneId))

  def printHttpDateString(d: ZonedDateTime): String = httpDateFormatter.format(d)

}
