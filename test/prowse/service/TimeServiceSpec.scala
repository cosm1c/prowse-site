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

  private val timeServiceComponent = new TimeServiceComponent {
    val timeService: TimeService = new TimeService {
      override val clock: Clock = mock[Clock].instant() returns earlierTime thenReturns laterTime
    }
  }

  def currentTime = {
    (timeServiceComponent.timeService.clock.instant() mustEqual earlierTime) and
      (timeServiceComponent.timeService.clock.instant() mustEqual laterTime)
  }

  def parseDateString = {
    val validDateString = "Sun, 16 Mar 2014 17:00:16 GMT"
    timeServiceComponent.timeService.parseDate(validDateString) must beSuccessfulTry
  }

  def formatDateString = {
    val formattedDateString = timeServiceComponent.timeService.formatHttpDate(earlierTime)
    timeServiceComponent.timeService.parseDate(formattedDateString) must beSuccessfulTry
  }

  def failParseDateString = {
    val invalidDateString = "This is not a valid date"
    timeServiceComponent.timeService.parseDate(invalidDateString) must beFailedTry
  }
}
