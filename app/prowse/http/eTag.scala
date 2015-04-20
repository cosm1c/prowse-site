package prowse.http

import scala.annotation.tailrec

/**
 * @see http://tools.ietf.org/html/draft-ietf-httpbis-p4-conditional#section-2.3
 */
sealed abstract class ETag(val value: String) {

  def strongComparison(that: ETag): Boolean

  def weakComparison(that: ETag): Boolean = this.value == that.value
}

case class StrongETag(override val value: String) extends ETag(value) {

  // TODO: Can we optimise away typecheck? (overloading?)
  override def strongComparison(that: ETag): Boolean = that match {
    case StrongETag(rVal) => value == rVal
    case _ => false
  }

  override def toString: String = s"""\"$value\""""
}

case class WeakETag(override val value: String) extends ETag(value) {

  override def strongComparison(that: ETag): Boolean = false

  override def toString: String = s"""W/\"$value\""""
}

object StarETag extends ETag("*") {

  override def strongComparison(that: ETag): Boolean = true

  override def weakComparison(that: ETag): Boolean = true
}

case class InvalidETag(msg: String)

object ETag {
  private val SUCCESSFUL_STAR_ETAG = Right(StarETag)
  private val STAR_HEADER_SEQ: Seq[ETag] = Seq(StarETag)
  private val EMPTY_ERROR = Left(InvalidETag("Empty value for ETag"))
  private val MISSING_OPEN_QUOTE_ERROR = Left(InvalidETag("Illegal value for ETag - no open quote"))
  private val MISSING_QUOTES_ERROR = Left(InvalidETag("Illegal value for ETag - not enclosed in quotes"))
  private val INVALID_STRONG_PREFIX_ERROR = Left(InvalidETag("Illegal value for ETag - unknown leading text for StrongETag"))
  private val INVALID_WEAK_PREFIX_ERROR = Left(InvalidETag("Illegal value for ETag - unknown leading text for WeakETag"))

  def parse(value: String): Either[InvalidETag, ETag] = {
    val endIndex = findLastNonWhitespace(value)

    if (endIndex == -1)
      EMPTY_ERROR

    else {
      val startQuoteIndex = value.lastIndexOf('"', endIndex - 1)

      if (startQuoteIndex == -1) {
        if (value.charAt(endIndex) == '*')
          SUCCESSFUL_STAR_ETAG
        else
          MISSING_OPEN_QUOTE_ERROR

      } else if (value.charAt(startQuoteIndex) != '"' || value.charAt(endIndex) != '"') {
        MISSING_QUOTES_ERROR

      } else {
        if (startQuoteIndex > 1
          && value.charAt(startQuoteIndex - 1) == '/'
          && value.charAt(startQuoteIndex - 2) == 'W') {
          if (findLastNonWhitespace(value, startQuoteIndex - 3) != -1)
            INVALID_STRONG_PREFIX_ERROR
          else
            Right(WeakETag(value.substring(startQuoteIndex + 1, endIndex)))

        } else {
          if (findLastNonWhitespace(value, startQuoteIndex - 1) != -1)
            INVALID_WEAK_PREFIX_ERROR
          else
            Right(StrongETag(value.substring(startQuoteIndex + 1, endIndex)))
        }
      }
    }
  }

  def parseETagsHeader(header: String): Seq[ETag] = {
    val currIndex = findLastNonWhitespace(header)
    if (currIndex == -1) {
      Nil

    } else {
      header.charAt(currIndex) match {
        case '"' => parseETagHeaderEntry(List(), header, currIndex)
        case '*' if findLastNonWhitespace(header, currIndex - 1) == -1 => STAR_HEADER_SEQ
        case _ => Nil
      }
    }
  }

  @tailrec private def parseETagHeaderEntry(eTags: List[ETag], header: String, endIndex: Int): Seq[ETag] = {
    val startQuoteIndex = header.lastIndexOf('"', endIndex - 1)
    if (startQuoteIndex == -1) {
      Nil

    } else {
      def appendETag: (List[ETag], Int) =
        if (startQuoteIndex > 1
          && header.charAt(startQuoteIndex - 1) == '/'
          && header.charAt(startQuoteIndex - 2) == 'W')
          (WeakETag(header.substring(startQuoteIndex + 1, endIndex)) :: eTags,
            findLastNonWhitespace(header, startQuoteIndex - 3))
        else
          (StrongETag(header.substring(startQuoteIndex + 1, endIndex)) :: eTags,
            findLastNonWhitespace(header, startQuoteIndex - 1))

      appendETag match {
        case (newETags, nextIndex) =>
          if (nextIndex == -1)
            newETags

          else if (header.charAt(nextIndex) != ',')
            Nil

          else {
            val nextEndIndex = findLastNonWhitespace(header, nextIndex - 1)
            if (nextEndIndex == -1 || header.charAt(nextEndIndex) != '"')
              Nil
            else
              parseETagHeaderEntry(newETags, header, nextEndIndex)
          }
      }
    }
  }

  private def findLastNonWhitespace(value: String): Int = findLastNonWhitespace(value, value.length - 1)

  @tailrec private def findLastNonWhitespace(value: String, offset: Int): Int = {
    if (offset < 0 || !isLinearWhiteSpace(value.charAt(offset))) {
      offset
    } else {
      findLastNonWhitespace(value, offset - 1)
    }
  }

  /** @see https://tools.ietf.org/html/rfc2616#section-4.2 */
  private def isLinearWhiteSpace(ch: Char): Boolean = ch == ' ' || ch == '\t'

}
