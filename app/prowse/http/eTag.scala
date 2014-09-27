package prowse.http

import scala.annotation.tailrec
import scala.util.{Success, Try}

/**
 * @see http://tools.ietf.org/html/draft-ietf-httpbis-p4-conditional#section-2.3
 */
sealed trait ETag {
  def value: String

  def display: String

  def strongComparison(that: ETag): Boolean = (this, that) match {
    case (StrongETag(lVal, _), StrongETag(rVal, _)) => lVal == rVal
    case _ => false
  }

  def weakComparison(that: ETag): Boolean = this.value == that.value
}

case class StrongETag(override val value: String, override val display: String) extends ETag {
  def this(value: String) = this(value, s"""\"$value\"""")
}

case class WeakETag(override val value: String, override val display: String) extends ETag {
  def this(value: String) = this(value, s"""W/\"$value\"""")
}

object StarETag extends ETag {
  val value = "*"

  val display = value

  override def strongComparison(that: ETag): Boolean = true

  override def weakComparison(that: ETag): Boolean = true
}

object StrongETag {
  def apply(string: String): StrongETag = {
    new StrongETag(string)
  }
}

object WeakETag {
  def apply(string: String): WeakETag = {
    new WeakETag(string)
  }
}

object ETag {
  private val STAR_ETAG = Success(StarETag)
  private val EMPTY_HEADER: Option[Seq[ETag]] = Some(Nil)
  private val STAR_HEADER: Option[Seq[ETag]] = Some(Seq(StarETag))

  def parse(value: String): Try[ETag] = {
    val endIndex = findLastNonWhitespace(value)

    if (endIndex == -1)
      throw new IllegalArgumentException("Empty value for ETag")

    else {
      val startQuoteIndex = value.lastIndexOf('"', endIndex - 1)

      if (startQuoteIndex == -1) {
        if (value.charAt(endIndex) == '*')
          STAR_ETAG
        else
          throw new IllegalArgumentException(s"Illegal value for ETag - no open quote: $value")

      } else if (value.charAt(startQuoteIndex) != '"' || value.charAt(endIndex) != '"') {
        throw new IllegalArgumentException(s"Illegal value for ETag: $value")

      } else {
        if (startQuoteIndex > 1
          && value.charAt(startQuoteIndex - 1) == '/'
          && value.charAt(startQuoteIndex - 2) == 'W') {
          if (findLastNonWhitespace(value, startQuoteIndex - 3) != -1)
            throw new IllegalArgumentException(s"Illegal value for ETag: $value")
          else
            Success(WeakETag(value.substring(startQuoteIndex + 1, endIndex), value.substring(startQuoteIndex - 2, endIndex + 1)))

        } else {
          if (findLastNonWhitespace(value, startQuoteIndex - 1) != -1)
            throw new IllegalArgumentException(s"Illegal value for ETag: $value")
          else
            Success(StrongETag(value.substring(startQuoteIndex + 1, endIndex), value.substring(startQuoteIndex, endIndex + 1)))
        }
      }
    }
  }

  def parseETagsHeader(header: String): Option[Seq[ETag]] = {
    val currIndex = findLastNonWhitespace(header)
    if (currIndex == -1) {
      EMPTY_HEADER

    } else {
      header.charAt(currIndex) match {
        case '"' => parseETagHeaderEntry(List(), header, currIndex)
        case '*' if findLastNonWhitespace(header, currIndex - 1) == -1 => STAR_HEADER
        case _ => EMPTY_HEADER
      }
    }
  }

  @tailrec private def parseETagHeaderEntry(eTags: List[ETag], header: String, endIndex: Int): Option[Seq[ETag]] = {
    val startQuoteIndex = header.lastIndexOf('"', endIndex - 1)
    if (startQuoteIndex == -1) {
      EMPTY_HEADER

    } else {
      def appendETag: (List[ETag], Int) =
        if (startQuoteIndex > 1
          && header.charAt(startQuoteIndex - 1) == '/'
          && header.charAt(startQuoteIndex - 2) == 'W')
          (WeakETag(header.substring(startQuoteIndex + 1, endIndex), header.substring(startQuoteIndex - 2, endIndex + 1)) :: eTags,
            findLastNonWhitespace(header, startQuoteIndex - 3))
        else
          (StrongETag(header.substring(startQuoteIndex + 1, endIndex), header.substring(startQuoteIndex, endIndex + 1)) :: eTags,
            findLastNonWhitespace(header, startQuoteIndex - 1))

      appendETag match {
        case (newETags, nextIndex) =>
          if (nextIndex == -1)
            Some(newETags)

          else if (header.charAt(nextIndex) != ',')
            EMPTY_HEADER

          else {
            val nextEndIndex = findLastNonWhitespace(header, nextIndex - 1)
            if (nextEndIndex == -1 || header.charAt(nextEndIndex) != '"')
              EMPTY_HEADER
            else
              parseETagHeaderEntry(newETags, header, nextEndIndex)
          }
      }
    }
  }

  private def findLastNonWhitespace(value: String): Int = findLastNonWhitespace(value, value.length - 1)

  private def findLastNonWhitespace(value: String, offset: Int): Int = {
    var result: Int = offset
    while (result >= 0) {
      if (!Character.isWhitespace(value.charAt(result))) {
        return result
      }
      result = result - 1
    }
    result
  }

}
