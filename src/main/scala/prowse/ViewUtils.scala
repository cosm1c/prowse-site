package prowse

import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

import play.twirl.api.Html

object ViewUtils {

  val displayDateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME

  val htmlDateAndTimeFormatter = DateTimeFormatter.ISO_INSTANT

  def timeElement(dateTime: TemporalAccessor): Html = Html(
    s"""<time datetime="${htmlDateAndTimeFormatter.format(dateTime)}">${displayDateTimeFormatter.format(dateTime)}</time>"""
  )

}
