package prowse.domain

import java.time.{Instant, ZoneId, ZonedDateTime}

import buildinfo.BuildInfo

object BuildInfoHelper {

  val buildDateTime: ZonedDateTime = Instant.parse(BuildInfo.buildInstant).atZone(ZoneId.of("GMT"))

}
