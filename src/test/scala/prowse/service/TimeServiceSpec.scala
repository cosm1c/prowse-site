package prowse.service

import java.time.Month.SEPTEMBER
import java.time.ZoneOffset.UTC
import java.time.{Clock, Instant, LocalDateTime}

import org.specs2.Specification
import org.specs2.mock.Mockito

class TimeServiceSpec extends Specification with Mockito {
  def is = s2"""
  provide current time                                                                 $currentTime
  parses http date string                                                              $parseDateString
  formats date string                                                                  $formatDateString
  return None when fails to parse http date string                                     $failParseDateString
  """

  private val earlierTime: Instant = LocalDateTime.of(2014, SEPTEMBER, 27, 4, 5, 1).toInstant(UTC)
  private val laterTime: Instant = LocalDateTime.of(2014, SEPTEMBER, 27, 4, 5, 2).toInstant(UTC)
  assert(earlierTime isBefore laterTime)

  private val timeService: TimeService = new TimeService {
    private val mockClock = mock[Clock].instant() returns earlierTime thenReturns laterTime

    override def clock: Clock = mockClock
  }

  def currentTime = {
    (timeService.clock.instant() mustEqual earlierTime) and
      (timeService.clock.instant() mustEqual laterTime)
  }

  def parseDateString = {
    val validDateString = "Sun, 28 Sep 2014 10:02:35 GMT"
    timeService.parseDate(validDateString) must beSuccessfulTry
  }

  def formatDateString = {
    val formattedDateString = timeService.formatHttpDate(earlierTime)
    timeService.parseDate(formattedDateString) must beSuccessfulTry
  }

  def failParseDateString = {
    val invalidDateString = "This is not a valid date"
    timeService.parseDate(invalidDateString) must beFailedTry
  }
}
