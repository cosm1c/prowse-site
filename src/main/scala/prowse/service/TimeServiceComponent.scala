package prowse.service

import java.time._
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Singleton

import play.api.http.HeaderNames._
import play.api.mvc.Result

import scala.util.Try

trait TimeOps {
  // TODO: must support all three date formats, see: http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics#section-7.1.1.1

  private val gmtZoneId: ZoneId = ZoneId.of("GMT")

  /**
   * Format specified in Http spec.
   * eg: "Sun, 06 Nov 1994 08:49:37 GMT"
   * See: http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics-23#section-7.1.1
   */
  private val httpDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH)
      .withZone(gmtZoneId)

  // TODO: Remove atZone when JDK 8 for ARM is updated to at least build20
  def parseDate(date: String): Try[ZonedDateTime] = Try(LocalDateTime.parse(date, httpDateFormatter).atZone(gmtZoneId))

  def formatHttpDate(instant: Instant): String = httpDateFormatter.format(instant)
}

trait TimeService extends TimeOps {

  def clock: Clock

  def nowAsHttpDate: String = formatHttpDate(clock.instant())

  def dateHeader(block: => Result): Result = {
    block.withHeaders(DATE -> nowAsHttpDate)
  }
}

@Singleton
class SystemUTCClockTimeServiceComponent extends TimeService {
  override val clock = Clock.systemUTC()
}
