package prowse.domain

import java.time.{Instant, ZoneId, ZonedDateTime}

object BuildInfoHelper {

  val buildDateTime: ZonedDateTime = Instant.parse(BuildInfo.buildInstant).atZone(ZoneId.of("GMT"))

}
