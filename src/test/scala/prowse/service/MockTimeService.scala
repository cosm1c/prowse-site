package prowse.service

import java.time.{Clock, Instant}
import java.util.concurrent.atomic.AtomicLong

import org.specs2.mock.Mockito

/**
 * One second always passes between calls to ensure different times
 */
object MockTimeService extends TimeService with Mockito {

  private val startTime: Instant = Instant.now()
  private val count = new AtomicLong
  private val mockClock = mock[Clock]

  private def nextInstant: Instant = {
    startTime.plusSeconds(count.getAndIncrement())
  }

  mockClock.instant answers { _ => nextInstant }

  override def clock: Clock = mockClock

  def lastTime: Instant = startTime.plusSeconds(count.get())
}
