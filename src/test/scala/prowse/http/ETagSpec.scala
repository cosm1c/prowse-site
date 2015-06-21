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
    StrongETag("strongValue").toString === "\"strongValue\""
  }

  def e2 = {
    WeakETag("weakValue").toString === "W/\"weakValue\""
  }

  def e3 = {
    (ETag.parse("W/\"weakValue\"") must beRight(WeakETag("weakValue"))) and
      (ETag.parse("W/\"weakValue\" ") must beRight(WeakETag("weakValue"))) and
      (ETag.parse(" W/\"weakValue\"") must beRight(WeakETag("weakValue")))
  }

  def e4 = {
    (ETag.parse("\"strongValue\"") must beRight(StrongETag("strongValue"))) and
      (ETag.parse(" \"strongValue\"") must beRight(StrongETag("strongValue"))) and
      (ETag.parse("\"strongValue\" ") must beRight(StrongETag("strongValue")))
  }

  def e5 = {
    (ETag.parse("noStrongStartQuote\"") must beLeft) and
      (ETag.parse("\"noStrongEndQuote") must beLeft) and
      (ETag.parse("W/noWeakStartQuote") must beLeft) and
      (ETag.parse("W/\"noWeakEndQuote") must beLeft)
  }

  def e6 = {
    val eTagDisplay = "\"" + allowedETagChars + '"'

    ETag.parse(eTagDisplay) must beRight(StrongETag(allowedETagChars))
  }

  def e7 = {
    val emptyETagDisplay: String = "\"\""
    ETag.parse(emptyETagDisplay) must beRight(StrongETag(""))
  }

  def e8 = {
    (ETag.parse("*") must beRight(StarETag)) and
      (ETag.parse(" *") must beRight(StarETag)) and
      (ETag.parse("* ") must beRight(StarETag))
  }

  def e9 = {
    val eTagDisplay = "\"" + allowedETagChars + '"'
    val eTagHeaderValue: String = s"""$eTagDisplay, $eTagDisplay, """""
    val expectedResult: Seq[ETag] = Seq(StrongETag(allowedETagChars), StrongETag(allowedETagChars), StrongETag(""))

    ETag.parseETagsHeader(eTagHeaderValue) === expectedResult
  }

  def e10 = {
    (ETag.parseETagsHeader(" ") must beEmpty) and
      (ETag.parseETagsHeader( """"Valid", INVALID, "Valid2"""") must beEmpty) and
      (ETag.parseETagsHeader( """"Valid", /"INVALID", "Valid2"""") must beEmpty) and
      (ETag.parseETagsHeader( """"Valid", W"INVALID", "Valid2"""") must beEmpty) and
      (ETag.parseETagsHeader( """"Valid", W/INVALID, "Valid2"""") must beEmpty) and
      (ETag.parseETagsHeader( """"Valid", W/"INVALID, "Valid2"""") must beEmpty) and
      (ETag.parseETagsHeader( """"Valid", "Valid2",""") must beEmpty) and
      (ETag.parseETagsHeader( """"Valid", "INVALID""") must beEmpty) and
      (ETag.parseETagsHeader( """"Valid",""") must beEmpty) and
      (ETag.parseETagsHeader( """"Valid", """) must beEmpty) and
      (ETag.parseETagsHeader( ""","Valid"""") must beEmpty) and
      (ETag.parseETagsHeader( """ ,"Valid"""") must beEmpty) and
      (ETag.parseETagsHeader("\"") must beEmpty) and
      (ETag.parseETagsHeader( """a*""") must beEmpty) and
      (ETag.parseETagsHeader( """*a""") must beEmpty) and
      (ETag.parseETagsHeader( """*, "INVALID""") must beEmpty)
  }

  def e11 = {
    val expectedResult: Seq[ETag] = Seq(StrongETag("Valid"), StrongETag(""), StrongETag("Valid2"))
    (ETag.parseETagsHeader( """"Valid", "", "Valid2"""") === expectedResult) and
      (ETag.parseETagsHeader("") === Nil)
  }

  def e12 = {
    val strongThenWeak: Seq[ETag] = Seq(StrongETag("STRONG"), WeakETag("WEAK"))
    val weakThenStrong: Seq[ETag] = Seq(WeakETag("WEAK"), StrongETag("STRONG"))
    (ETag.parseETagsHeader( """"STRONG", W/"WEAK"""") === strongThenWeak) and
      (ETag.parseETagsHeader( """"STRONG" ,W/"WEAK"""") === strongThenWeak) and
      (ETag.parseETagsHeader( """"STRONG" , W/"WEAK"""") === strongThenWeak) and
      (ETag.parseETagsHeader( """"STRONG",W/"WEAK"""") === strongThenWeak) and
      (ETag.parseETagsHeader( """ "STRONG",W/"WEAK"""") === strongThenWeak) and
      (ETag.parseETagsHeader( """"STRONG",W/"WEAK" """) === strongThenWeak) and
      (ETag.parseETagsHeader( """W/"WEAK","STRONG"""") === weakThenStrong) and
      (ETag.parseETagsHeader( """W/"WEAK" ,"STRONG"""") === weakThenStrong) and
      (ETag.parseETagsHeader( """W/"WEAK", "STRONG"""") === weakThenStrong) and
      (ETag.parseETagsHeader( """W/"WEAK" , "STRONG"""") === weakThenStrong) and
      (ETag.parseETagsHeader( """ W/"WEAK","STRONG"""") === weakThenStrong) and
      (ETag.parseETagsHeader( """W/"WEAK","STRONG" """) === weakThenStrong)
  }

  def e13 = {
    def isStarETag(header: Seq[ETag]) = header === Seq(StarETag)

    val simple: Seq[ETag] = ETag.parseETagsHeader("*")
    val leadingSpace: Seq[ETag] = ETag.parseETagsHeader(" *")
    val trailingSpace: Seq[ETag] = ETag.parseETagsHeader("* ")

    isStarETag(simple) and isStarETag(leadingSpace) and isStarETag(trailingSpace)
  }

  def e14 = {
    ETag.parseETagsHeader("asd*") must beEmpty
  }

  private val allowedETagChars: String = new String((Seq(0x21) ++ Range(0x23, 0x7E).toSeq).map(_.toChar).toArray)
}
