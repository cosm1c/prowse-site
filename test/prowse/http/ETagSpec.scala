package prowse.http

import org.specs2.{Specification, matcher}

class ETagSpec extends Specification with matcher.DataTables {
  def is = s2"""
  StrongETag should display strong eTag                                 $e1
  WeakETag should display weak eTag                                     $e2
  ETag with strong value should be StrongETag                           $e3
  ETag with weak value should be WeakETag                               $e4
  Parsing
    rejects invalid ETag values                                         $e5
    supports all ascii chars 0x21,0x23-0x7E                             $e6
    support empty value                                                 $e7
    supports star                                                       $e8
  Parsing HTTP Headers
    supports all ascii chars 0x21,0x23-0x7E                             $e9
    ignores all when array contains invalid entries                     $e10
    support empty value                                                 $e11
    supports combination of both Strong and Weak ETags                  $e12
    supports star                                                       $e13
    ignores invalid star                                                $e14

  eTag comparison table - see http://tools.ietf.org/html/draft-ietf-httpbis-p4-conditional#section-2.3.2
  """ ^ {
    p
    "ETag 1" | "ETag 2" | "Strong Comparison" | "Weak Comparison" |
      WeakETag("1") ! WeakETag("1") ! false ! true |
      WeakETag("1") ! WeakETag("2") ! false ! false |
      WeakETag("1") ! StrongETag("1") ! false ! true |
      StrongETag("1") ! StrongETag("1") ! true ! true |> {
      (a, b, x, y) => ((a strongComparison b) must_== x) and ((a weakComparison b) must_== y)
    }
  }

  def e1 = {
    StrongETag("strongValue").display === "\"strongValue\""
  }

  def e2 = {
    WeakETag("weakValue").display === "W/\"weakValue\""
  }

  def e3 = {
    (ETag.parse("W/\"weakValue\"") must beSuccessfulTry.withValue(WeakETag("weakValue", "W/\"weakValue\""))) and
      (ETag.parse("W/\"weakValue\" ") must beSuccessfulTry.withValue(WeakETag("weakValue", "W/\"weakValue\""))) and
      (ETag.parse(" W/\"weakValue\"") must beSuccessfulTry.withValue(WeakETag("weakValue", "W/\"weakValue\"")))
  }

  def e4 = {
    (ETag.parse("\"strongValue\"") must beSuccessfulTry.withValue(StrongETag("strongValue", "\"strongValue\""))) and
      (ETag.parse(" \"strongValue\"") must beSuccessfulTry.withValue(StrongETag("strongValue", "\"strongValue\""))) and
      (ETag.parse("\"strongValue\" ") must beSuccessfulTry.withValue(StrongETag("strongValue", "\"strongValue\"")))
  }

  def e5 = {
    (ETag.parse("noStrongStartQuote\"") must throwA[java.lang.IllegalArgumentException]) and
      (ETag.parse("\"noStrongEndQuote") must throwA[java.lang.IllegalArgumentException]) and
      (ETag.parse("W/noWeakStartQuote") must throwA[java.lang.IllegalArgumentException]) and
      (ETag.parse("W/\"noWeakEndQuote") must throwA[java.lang.IllegalArgumentException])
  }

  def e6 = {
    val eTagDisplay = "\"" + allowedETagChars + '"'

    ETag.parse(eTagDisplay) must beSuccessfulTry.withValue(StrongETag(allowedETagChars, eTagDisplay))
  }

  def e7 = {
    val emptyETagDisplay: String = "\"\""
    ETag.parse(emptyETagDisplay) must beSuccessfulTry.withValue(StrongETag("", emptyETagDisplay))
  }

  def e8 = {
    (ETag.parse("*") must beSuccessfulTry.withValue(StarETag)) and
      (ETag.parse(" *") must beSuccessfulTry.withValue(StarETag)) and
      (ETag.parse("* ") must beSuccessfulTry.withValue(StarETag))
  }

  def e9 = {
    val eTagDisplay = "\"" + allowedETagChars + '"'
    val eTagHeaderValue: String = s"""$eTagDisplay, $eTagDisplay, """""
    val expectedResult: Seq[ETag] = Seq(StrongETag(allowedETagChars, eTagDisplay), StrongETag(allowedETagChars, eTagDisplay), StrongETag("", "\"\""))

    val eTagsHeader: Option[Seq[ETag]] = ETag.parseETagsHeader(eTagHeaderValue)
    (eTagsHeader must beSome) and
      (eTagsHeader.get === expectedResult)
  }

  def e10 = {
    (ETag.parseETagsHeader(" ") must beSome.which(_.isEmpty)) and
      (ETag.parseETagsHeader( """"Valid", INVALID, "Valid2"""") must beSome.which(_.isEmpty)) and
      (ETag.parseETagsHeader( """"Valid", /"INVALID", "Valid2"""") must beSome.which(_.isEmpty)) and
      (ETag.parseETagsHeader( """"Valid", W"INVALID", "Valid2"""") must beSome.which(_.isEmpty)) and
      (ETag.parseETagsHeader( """"Valid", W/INVALID, "Valid2"""") must beSome.which(_.isEmpty)) and
      (ETag.parseETagsHeader( """"Valid", W/"INVALID, "Valid2"""") must beSome.which(_.isEmpty)) and
      (ETag.parseETagsHeader( """"Valid", "Valid2",""") must beSome.which(_.isEmpty)) and
      (ETag.parseETagsHeader( """"Valid", "INVALID""") must beSome.which(_.isEmpty)) and
      (ETag.parseETagsHeader( """"Valid",""") must beSome.which(_.isEmpty)) and
      (ETag.parseETagsHeader( """"Valid", """) must beSome.which(_.isEmpty)) and
      (ETag.parseETagsHeader( ""","Valid"""") must beSome.which(_.isEmpty)) and
      (ETag.parseETagsHeader( """ ,"Valid"""") must beSome.which(_.isEmpty)) and
      (ETag.parseETagsHeader("\"") must beSome.which(_.isEmpty)) and
      (ETag.parseETagsHeader( """a*""") must beSome.which(_.isEmpty)) and
      (ETag.parseETagsHeader( """*a""") must beSome.which(_.isEmpty)) and
      (ETag.parseETagsHeader( """*, "INVALID""") must beSome.which(_.isEmpty))
  }

  def e11 = {
    val expectedResult: Seq[ETag] = Seq(StrongETag("Valid", "\"Valid\""), StrongETag("", "\"\""), StrongETag("Valid2", "\"Valid2\""))
    ETag.parseETagsHeader( """"Valid", "", "Valid2"""") must beSome.which(_ == expectedResult)
  }

  def e12 = {
    val strongThenWeak: Seq[ETag] = Seq(StrongETag("STRONG", "\"STRONG\""), WeakETag("WEAK", "W/\"WEAK\""))
    val weakThenStrong: Seq[ETag] = Seq(WeakETag("WEAK", "W/\"WEAK\""), StrongETag("STRONG", "\"STRONG\""))
    (ETag.parseETagsHeader( """"STRONG", W/"WEAK"""") must beSome.which(_ == strongThenWeak)) and
      (ETag.parseETagsHeader( """"STRONG" ,W/"WEAK"""") must beSome.which(_ == strongThenWeak)) and
      (ETag.parseETagsHeader( """"STRONG" , W/"WEAK"""") must beSome.which(_ == strongThenWeak)) and
      (ETag.parseETagsHeader( """"STRONG",W/"WEAK"""") must beSome.which(_ == strongThenWeak)) and
      (ETag.parseETagsHeader( """ "STRONG",W/"WEAK"""") must beSome.which(_ == strongThenWeak)) and
      (ETag.parseETagsHeader( """"STRONG",W/"WEAK" """) must beSome.which(_ == strongThenWeak)) and
      (ETag.parseETagsHeader( """W/"WEAK","STRONG"""") must beSome.which(_ == weakThenStrong)) and
      (ETag.parseETagsHeader( """W/"WEAK" ,"STRONG"""") must beSome.which(_ == weakThenStrong)) and
      (ETag.parseETagsHeader( """W/"WEAK", "STRONG"""") must beSome.which(_ == weakThenStrong)) and
      (ETag.parseETagsHeader( """W/"WEAK" , "STRONG"""") must beSome.which(_ == weakThenStrong)) and
      (ETag.parseETagsHeader( """ W/"WEAK","STRONG"""") must beSome.which(_ == weakThenStrong)) and
      (ETag.parseETagsHeader( """W/"WEAK","STRONG" """) must beSome.which(_ == weakThenStrong))
  }

  def e13 = {
    def isStarETag(header: Option[Seq[ETag]]) = (header must beSome) and (header.get === Seq(StarETag))

    val simple: Option[Seq[ETag]] = ETag.parseETagsHeader("*")
    val leadingSpace: Option[Seq[ETag]] = ETag.parseETagsHeader(" *")
    val trailingSpace: Option[Seq[ETag]] = ETag.parseETagsHeader("* ")

    isStarETag(simple) and isStarETag(leadingSpace) and isStarETag(trailingSpace)
  }

  def e14 = {
    (ETag.parseETagsHeader("asd*") must beSome) and
      ETag.parseETagsHeader("asd*").get.isEmpty
  }

  private val allowedETagChars: String = new String((Seq(0x21) ++ Range(0x23, 0x7E).toSeq).map(_.toChar).toArray)
}
