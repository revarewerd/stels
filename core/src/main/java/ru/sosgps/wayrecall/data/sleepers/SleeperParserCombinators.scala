package ru.sosgps.wayrecall.data.sleepers

import java.net.URL
import java.util.regex.Matcher

import ru.sosgps.wayrecall.data.sleepers.StarLineSleeperParserCombinator.{batPercentageKey, batValueKey, deviceName, fullMsg, lbs, lbsKey, lonlatKey, parseAll, positionURLKey, regexMatch}
import ru.sosgps.wayrecall.utils.KTMap.Pair
import ru.sosgps.wayrecall.utils._

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers

trait SleeperParserCombinator extends RegexParsers {

  def regexMatch(r: Regex): Parser[Matcher] = new Parser[Matcher] {
    def apply(in: Input) = {
      val source = in.source
      val offset = in.offset
      val start = handleWhiteSpace(source, offset)

      val matcher = r.pattern.matcher(source.subSequence(start, source.length))
      if (matcher.find(0) && matcher.start() == 0) {
        Success(matcher,
          in.drop(start + matcher.end - offset))
      } else
        Failure("string matching regex `" + r + "' expected but `" + in.first + "' found", in.drop(start - offset))
    }
  }

  implicit class matchOps(val m: Matcher) {
    def gopt(gname: String): Option[String] = try {
      Some(m.group(gname))
    } catch {
      case e: NoSuchElementException => None
    }
  }

  def apply(input: String): ParseResult[MatchResult]


  val lonlatKey = KTMap.key[(Double,Double)]
  val positionURLKey = KTMap.key[URL]
  val alarmKey = KTMap.key[Alarm]
  val lbsKey = KTMap.key[LBS]
  val batValueKey = KTMap.key[String]
  val batPercentageKey = KTMap.key[String]

}


trait CoordinatesParser extends SleeperParserCombinator {

  private def coordinate = regexMatch("""(?<L>E|N)(?<V>[\d|\.]+)""".r) ^^ { m =>
    m.group("L") -> m.group("V").toDouble
  }

  def lonlat: Parser[KTMap.Pair[(Double, Double)]] = lonlatC | (
    coordinate ~ coordinate ^? {
      case c1 ~ c2 if c1._1 == "E" && c2._1 == "N" => lonlatKey -> (c1._2, c2._2)
      case c2 ~ c1 if c1._1 == "E" && c2._1 == "N" => lonlatKey -> (c1._2, c2._2)
    }
    )

  private val calibrusGPS = """.*N(\d+ \d+\.\d+) E(\d+ \d+\.\d+).*""".r
  private val dotless = """.*N(\d+ \d+) E(\d+ \d+).*""".r

  private val proma2GPS = """.*N(\d+\.\d+) E(\d+\.\d+).*""".r

  def lonlatC: Parser[KTMap.Pair[(Double, Double)]]  =
    """N(?<N>[\d|\.| ]+) E(?<E>[\d|\.| ]+)""".r ^? {
      case calibrusGPS(n, e) =>
        val ns = n.split(" ")
        val lat = ns(0).toDouble + ns(1).toDouble / 60
        val es = e.split(" ")
        val lon = es(0).toDouble + es(1).toDouble / 60
        lonlatKey -> (lon, lat)
      case dotless(n, e) =>
        val ns = n.split(" ")
        val lat = ns(0).toDouble + ns(1).toDouble / 60
        val es = e.split(" ")
        val lon = es(0).toDouble + es(1).toDouble / 60
        lonlatKey -> (lon, lat)
      case proma2GPS(n, e) =>
        lonlatKey -> (e.toDouble, n.toDouble)
    }
}

object VegaSleeperParserCombinator extends RegexParsers with CoordinatesParser {
  def date =  """\d\d\.\d\d\.\d\d \d\d:\d\d:\d\d""".r ^^ (_ => KTMap.empty)

  def coordinates =
    regexMatch("""N (?<lat>\d+.\d+) E (?<lon>\d+.\d+)""".r) ^^ (
      g =>
        KTMap(lonlatKey -> (g.group("lon").toDouble, g.group("lat").toDouble)))

  def skip = """\S+""".r ^^ (_ => KTMap.empty)

  def bat =
    regexMatch("""A/h=(?<batproc>\d+%)""".r) ^^ (g => KTMap(batPercentageKey -> g.group("batproc")))

  def lbs: Parser[Pair[LBS]] =
    """(:?LBS: )?MCC[=|:] ?(?<MCC>\d+) MNC[=|:] ?(?<MNC>\d+) LAC[=|:] ?(?<LAC>([0-9a-fA-F\.]{3,} ?)*) CID[=|:] ?(?<CID>([0-9a-fA-F\.]{3,} ?)*)""".r ^^ (lbsString => {
    //debug(s"lbsString: $lbsString")
    val regexp = """MCC[=|:] ?(?<MCC>\d+) MNC[=|:] ?(?<MNC>\d+) LAC[=|:] ?(?<LAC>.*) CID[=|:] ?(?<CID>.*)""".r
    val matcher = regexp.pattern.matcher(lbsString)
    assert(matcher.matches())
    lbsKey -> LBS(
      tryParseInt(matcher.group("MCC")).getOrElse(-1),
      tryParseInt(matcher.group("MNC")).getOrElse(-1),
      tryParseIntBased(matcher.group("LAC").split(" ")(0), 16).getOrElse(-1),
      tryParseIntBased(matcher.group("CID").split(" ")(0), 16).getOrElse(-1)
    )
  })

  val fullMsg = {
    date ~ (lbs | bat | coordinates | skip).+ ^^ {
      case d ~ rest => {
        val kTMap = rest.reduce(_ ++ _)
        new MatchResult {
          override def lonlat: Option[(Double, Double)] = kTMap.get(lonlatKey)

          override def positionURL: Option[URL] = None

          override def alarm: Option[Alarm] = None

          override def lbs: Option[LBS] = kTMap.get(lbsKey)

          override def batValue: Option[String] = None

          override def batPercentage: Option[String] = kTMap.get(batPercentageKey)
        }
      }
    }
  }

    override def apply(input: String): ParseResult[MatchResult] = parseAll(fullMsg,input)

  }

object StarLineSleeperParserCombinator extends RegexParsers with CoordinatesParser {
  def deviceName = """StarLine M\d+ \S+""".r ^^ (_ => KTMap.empty)

  def skip = """\S+""".r ^^ (_ => KTMap.empty)

  def lbs: Parser[Pair[LBS]] =
    """(:?LBS: )?MCC[=|:] ?(?<MCC>\d+) MNC[=|:] ?(?<MNC>\d+) LAC[=|:] ?(?<LAC>([0-9a-fA-F\.]{3,} ?)*) CID[=|:] ?(?<CID>([0-9a-fA-F\.]{3,} ?)*)""".r ^^ (lbsString => {
      //debug(s"lbsString: $lbsString")
      val regexp = """(:?LBS: )?MCC[=|:] ?(?<MCC>\d+) MNC[=|:] ?(?<MNC>\d+) LAC[=|:] ?(?<LAC>.*) CID[=|:] ?(?<CID>.*)""".r
      val matcher = regexp.pattern.matcher(lbsString)
      assert(matcher.matches())
      lbsKey -> LBS(
        tryParseInt(matcher.group("MCC")).getOrElse(-1),
        tryParseInt(matcher.group("MNC")).getOrElse(-1),
        tryParseIntBased(matcher.group("LAC").split(" ")(0), 16).getOrElse(-1),
        tryParseIntBased(matcher.group("CID").split(" ")(0), 16).getOrElse(-1)
      )
    })

  def battery: Parser[KTMap] = regexMatch("""(Bat:|Battery: )(?<batval>\S+v) ?\((?<batproc>\d+%)\)""".r) ^^ (g => {
    KTMap(batValueKey -> g.group("batval"), batPercentageKey -> g.group("batproc"))
  }) | regexMatch("""Bat=(?<batproc>\d+%)""".r) ^^ (g => {
    KTMap(batPercentageKey -> g.group("batproc"))
  })

  val fullMsg = {
    deviceName ~ (lbs | battery | lonlat | skip ).+ ^^  {
      case devname ~ rest => {
        val kTMap = rest.reduce(_ ++ _)
        new MatchResult {
          override def lonlat: Option[(Double, Double)] = kTMap.get(lonlatKey)

          override def positionURL: Option[URL] = kTMap.get(positionURLKey)

          override def alarm: Option[Alarm] = None

          override def lbs: Option[LBS] = kTMap.get(lbsKey)

          override def batValue: Option[String] = kTMap.get(batValueKey)

          override def batPercentage: Option[String] = kTMap.get(batPercentageKey)
        }
      }
    }

  }


  override def apply(input: String): ParseResult[MatchResult] = parseAll(fullMsg,input)
}

//object FindMeSleeperParserCombinator extends RegexParsers with CoordinatesParser {
//  def deviceName = """StarLine M\d+ \S+""".r ^^ (_ => KTMap.empty)
//
//  def any = ".".r ^^ (_ => KTMap.empty)
//
//  def battery: Parser[KTMap] = regexMatch("""(Bat:|Battery: )(?<batval>\S+v) ?\((?<batproc>\d+%)\)""".r) ^^ (g => {
//    KTMap(batValueKey -> g.group("batval"), batPercentageKey -> g.group("batproc"))
//  }) | regexMatch("""Bat=(?<batproc>\d+%)""".r) ^^ (g => {
//    KTMap(batPercentageKey -> g.group("batproc"))
//  })
//
//  def lbs: Parser[Pair[LBS]] =
//    """(:?LBS: )?MCC[=|:] ?(?<MCC>\d+) MNC[=|:] ?(?<MNC>\d+) LAC[=|:] ?(?<LAC>([0-9a-fA-F\.]{3,} ?)*) CID[=|:] ?(?<CID>([0-9a-fA-F\.]{3,} ?)*)""".r ^^ (lbsString => {
//      //debug(s"lbsString: $lbsString")
//      val regexp = """(:?LBS: )?MCC[=|:] ?(?<MCC>\d+) MNC[=|:] ?(?<MNC>\d+) LAC[=|:] ?(?<LAC>.*) CID[=|:] ?(?<CID>.*)""".r
//      val matcher = regexp.pattern.matcher(lbsString)
//      assert(matcher.matches())
//      lbsKey -> LBS(
//        tryParseInt(matcher.group("MCC")).getOrElse(-1),
//        tryParseInt(matcher.group("MNC")).getOrElse(-1),
//        tryParseIntBased(matcher.group("LAC").split(" ")(0), 16).getOrElse(-1),
//        tryParseIntBased(matcher.group("CID").split(" ")(0), 16).getOrElse(-1)
//      )
//    })
//
//
//  val fullMsg = {
//    deviceName ~ (lbs | battery | any).+ ^^  {
//      case devname ~ rest => {
//        val kTMap = rest.reduce(_ ++ _)
//        new MatchResult {
//          override def lonlat: Option[(Double, Double)] = kTMap.get(lonlatKey)
//
//          override def positionURL: Option[URL] = kTMap.get(positionURLKey)
//
//          override def alarm: Option[Alarm] = None
//
//          override def lbs: Option[LBS] = kTMap.get(lbsKey)
//
//          override def batValue: Option[String] = kTMap.get(batValueKey)
//
//          override def batPercentage: Option[String] = kTMap.get(batPercentageKey)
//        }
//      }
//    }
//  }
//
//  override def apply(input: String): ParseResult[MatchResult] = parseAll(fullMsg,input)
//
//}

trait AutoPhoneSleeperParserCombinator extends RegexParsers with CoordinatesParser with grizzled.slf4j.Logging {

  def deviceName: Parser[Unit] = """(AGPS Mayak \S*|AutoF \S*|AutoFon SE-Mayak \S*|AutoFon Mayak \S*|SE-Mayak \S*|(AutoFon )?S?E?-Mayak \S*|AutoF \S*)""".r ^^ (_ => Unit)

  def daySmsTest = """\d+ day test sms.""".r ^^ (_ => KTMap.empty)

  def sat = """(Sat|GPS satellites): \d+ at \d+s\.""".r ^^ (_ => KTMap.empty)

  def gps = """GPS \S+s( No GPS data)?""".r ^^ (_ => KTMap.empty)

  def gsm = """GSM \S+dB""".r ^^ (_ => KTMap.empty)

  def date = """\d\d-\d\d-\d\d\d\d ?(\d\d:\d\d:\d\d|\d\d:\d\d)?""".r ^^ (_ => KTMap.empty) | """GMT \d\d\d\d\d\d \d\d:\d\d""".r ^^ (_ => KTMap.empty)

  def warning = """Warning: Low battery\.""".r ^^ (_ => KTMap.empty)

  def lbs: Parser[Pair[LBS]] =
    """(:?LBS: )?MCC[=|:] ?(?<MCC>\d+) MNC[=|:] ?(?<MNC>\d+) LAC[=|:] ?(?<LAC>([0-9a-fA-F\.]{3,} ?)*) CID[=|:] ?(?<CID>([0-9a-fA-F\.]{3,} ?)*)""".r ^^ (lbsString => {
      //debug(s"lbsString: $lbsString")
      val regexp = """(:?LBS: )?MCC[=|:] ?(?<MCC>\d+) MNC[=|:] ?(?<MNC>\d+) LAC[=|:] ?(?<LAC>.*) CID[=|:] ?(?<CID>.*)""".r
      val matcher = regexp.pattern.matcher(lbsString)
      assert(matcher.matches())
      lbsKey -> LBS(
        tryParseInt(matcher.group("MCC")).getOrElse(-1),
        tryParseInt(matcher.group("MNC")).getOrElse(-1),
        tryParseIntBased(matcher.group("LAC").split(" ")(0), 16).getOrElse(-1),
        tryParseIntBased(matcher.group("CID").split(" ")(0), 16).getOrElse(-1)
      )
    })

  def posurl = """http:\S+""".r ^^ (url => positionURLKey -> new URL(url))

  def movingSatus = """Speed: ?\d+km\/h(?: Course:\d+)?(?: Altitude:\w+)?(?: Accur:\d+m)?(, \d+)?""".r ^^ (_ => KTMap.empty)

  def addins = rep1("""\w+,\w+""".r ^^ (_ => KTMap.empty))

  def sensor = """Sensor=\w\/\w \(\w+\)""".r ^^ (_ => KTMap.empty)

  def battery: Parser[KTMap] = regexMatch("""(Bat:|Battery: )(?<batval>\S+v) ?\((?<batproc>\d+%)\)""".r) ^^ (g => {
    KTMap(batValueKey -> g.group("batval"), batPercentageKey -> g.group("batproc"))
  }) | regexMatch("""Bat=(?<batproc>\d+%)""".r) ^^ (g => {
    KTMap(batPercentageKey -> g.group("batproc"))
  })

  def temperature = """(T|Temp.): ?\S+C""".r ^^ (_ => KTMap.empty)

  def temperature44 = """t=[\+-]\d+C""".r ^^ (_ => KTMap.empty)

  def t1t2 = {
    val numstring = """: \d\d-\d\d-\d\d\d\d \d\d:\d\d,?""".r
    ("""T1""" ~ numstring ~ opt(addins) ~ opt("T2" ~ rep(numstring | """: \w{4}""".r) ~ opt(addins))) ^^ (_ => KTMap.empty)
  }

  def mode = """Mode[:=] ?\S+( \S+-)*( Move sensor: off)?( GPRS to server: off)?( ?RS to server: off)?""".r ^^ (_ => KTMap.empty)

  def sms = """sms# ?\d+""".r ^^ (_ => KTMap.empty)

  def tail = """\s*""".r ^^ (_ => KTMap.empty)

  //AutoFon E-Mayak v3.1d 23-08-2012 12:02 GSM -49dB LBS: MCC: 250 MNC: 001 LAC: 189C 189C 189C 189C CID: A20C A209 40BC 0D38 T1: 24-08-2012 12:00, 01D,S T2,G Battery: 5.46v (70%) Temp.: +22C sms# 59 Mode: sleep


  def attribute: Parser[KTMap] = daySmsTest | sat | gps | date | warning | t1t2 | lonlat | movingSatus | gsm | lbs | posurl | sensor | battery | temperature | temperature44 | mode | sms

  val fullSms: Parser[MatchResult] = deviceName ~ (attribute+) ~ tail ^^ {
    case (devName ~  attrs  ~ tail) =>

      val kTMap = attrs.reduce( _ ++ _)

      new MatchResult {
        override def lonlat: Option[(Double, Double)] = kTMap.get(lonlatKey)

        override def positionURL: Option[URL] = kTMap.get(positionURLKey)

        override def alarm: Option[Alarm] = None

        override def lbs: Option[LBS] = kTMap.get(lbsKey)

        override def batValue: Option[String] = kTMap.get(batValueKey)

        override def batPercentage: Option[String] = kTMap.get(batPercentageKey)
      }
  }

  def apply(input: String): ParseResult[MatchResult] = parseAll(fullSms, input)

}

trait AlphaMayakSleeperParserCombinator extends AutoPhoneSleeperParserCombinator {
  override def deviceName: Parser[Unit] = """Alfa-Mayak \S*""".r ^^ (_ => Unit)

  override def gsm: Parser[KTMap] = """GSM[:]?\s*\S+dB(\([^)]+\))?""".r ^^ (_ => KTMap.empty)


//  override def sms: Parser[KTMap] = super.sms

  override def date: Parser[KTMap] = """Time>\d\d-\d\d-\d\d \d\d:\d\d:\d\d""".r ^^ (_ => KTMap.empty)

  override def battery: Parser[KTMap] =
    regexMatch("""(Bat:|Battery: )(?<batval>\S+v) ?\((?<batproc>[^)]+)\)""".r) ^^ (g => {
    KTMap(batValueKey -> g.group("batval"), batPercentageKey -> g.group("batproc"))
  })

  def skip = """\S+""".r ^^ (_ => KTMap.empty)

  override def attribute: Parser[KTMap] = super.attribute | skip

}

object AutoPhoneSleeperParserCombinator extends AutoPhoneSleeperParserCombinator
object AlphaMayakSleeperParserCombinator extends AlphaMayakSleeperParserCombinator

object AutoPhoneRusSleeperParserCombinator extends AutoPhoneSleeperParserCombinator {

  override def deviceName: Parser[Unit] = """(Авто[фФ]он \S+ \S*)""".r ^^ (_ => KTMap.empty)

  override def date = """(\d\d\.\d\d\.\d\d\d\d)? ?(\d\d:\d\d:\d\d|\d\d:\d\d)""".r ^^ (_ => KTMap.empty) | super.date

  override def gsm = """GSM:\S+ Db""".r ^^ (_ => KTMap.empty) | super.gsm

  override def movingSatus = super.movingSatus | "Координаты GPS недоступны." ^^ (_ => KTMap.empty)

  override def sms = """(Sms# \d+)|(смс#\d+)""".r ^^ (_ => KTMap.empty)

  override def battery = regexMatch("""(Бат:)(?<batval>\S+в) ?\((?<batproc>\d+%)\)""".r) ^^ (g => {
     KTMap(batValueKey -> g.group("batval"), batPercentageKey -> g.group("batproc"))
  }) | regexMatch("""Bat=(?<batproc>\d+%)""".r) ^^ (g => {
    (batPercentageKey -> g.group("batproc"))
  }) | super.battery

}

object PromaSleeperParserCombinator extends RegexParsers with CoordinatesParser with grizzled.slf4j.Logging {

  def attribute: Parser[KTMap] = warning | date | ("No GPS data!" ^^ (_ => KTMap.empty)) | lbs | sat | gpslonlat | posurl | movingSatus | alarm | gsm | ("""FID:\S+""".r ^^ (_ => KTMap.empty)) |
    temperature | battery | addins | sms

  // (Colubris|PROMA SAT 777) \S*(:? Warning!\S+)?(:? \d+ day SMS test!)?(?: GMT\d+:\d+ \d+-\d+-\d+ \d+:\d+)?(?<GPS> GPS \S+ N\d+ [\d|\.]+ E\d+ [\d|\.]+ V=\d+ ?km/h \d+| No GPS data!)?(?<LBS> MCC:\d+ MNC:\d+ LAC:\w+ CID:\w+)?(:? GSM:\S+)?(?: ATM0:\S+\s+ATM1:\S+\s+)?(?: FID:\S+\s+)?Bat=(?<batval>\S+)-(?<batproc>\d+%) T=[-|\d|\.]+C #\d+SMS\s*
  val fullSms: Parser[MatchResult] = deviceName ~ (attribute +) ~ tail ^^ {
    case (devName ~ attributes ~ tail) =>

      val kTMap = attributes.reduce(_ ++ _)

      new MatchResult {
        override def lonlat: Option[(Double, Double)] = kTMap.get(lonlatKey)

        override def positionURL: Option[URL] = kTMap.get(positionURLKey)

        override def alarm: Option[Alarm] = kTMap.get(alarmKey)

        override def lbs: Option[LBS] = kTMap.get(lbsKey)

        override def batValue: Option[String] = kTMap.get(batValueKey)

        override def batPercentage: Option[String] = kTMap.get(batPercentageKey)
      }
  }

  def deviceName: Parser[KTMap] = """(Proma Sat \d+|PROMA SAT \d+|Colubris) V\S+ ?\D* """.r ^^ (_ => KTMap.empty)

  def warning: Parser[KTMap] = """(:? Warning!\S+)? ?(:?\d+ day SMS test!)""".r ^^ (_ => KTMap.empty)

  def date = """(GMT\d+:\d+ )?\d\d-\d\d-\d\d\d?\d? ?(\d\d:\d\d:\d\d|\d\d:\d\d)?""".r ^^ (_ => KTMap.empty)

  def sat = """Sat: \d+ at \d+s\.""".r ^^ (_ => KTMap.empty)

  def gpslonlat = """GPS \S+ """.r ~> lonlat

  def alarm: Parser[KTMap] = "Alarm:" ~> rep1(not(guard("GSM")) ~> """(Low )?\S*""".r) ^^ {
    case args =>
      val alarmString =  args.mkString(" ")
      //debug("alarmString:" + alarmString)
      if (alarmString.contains("Moving"))
        alarmKey -> Moving
      else if (alarmString.contains("Low Ex_Batt"))
        alarmKey -> Power
      else if (alarmString.contains("Light-Input"))
        alarmKey -> LightInput
      else
        KTMap.empty
  }

  def gsm = ("""GSM:\S+""".r ^^ (_ => KTMap.empty)) <~ opt("""ATM0:\S+\s+ATM1:\S+""".r)


  // MCC=250 MNC=1 LAC=18EC,CID=3F4D,-81dBm LAC1=FFFF,CID=FFFF,-63dBm LAC2=FFFF,CID=FFFF,-63dBm LAC3=FFFF,CID=FFFF,-63dBm
  def lbs: Parser[Pair[LBS]] =
  """(?<LBS>MCC[=:]\d+ MNC[=:]\d+ LAC[=:]\S+[ ,]CID[=:]\w+(,-?\d+dBm)?)( LAC\d=\S+,CID=\S+,-?\d+dBm)*""".r ^^ (lbsString => {
    val lbsPairs = lbsString.trim.split("[ ,]").take(4)
    //debug(s"lbsString: $lbsString pairs: ${lbsPairs.mkString(", ")}")
    val paramMap = lbsPairs.map(pariStr => {
      val pairSlit = pariStr.split("[=:]")
      (pairSlit(0), pairSlit(1))
    }).toMap
    lbsKey -> LBS(paramMap("MCC").toInt, paramMap("MNC").toInt, Integer.parseInt(paramMap("LAC"), 16), Integer.parseInt(paramMap("CID"), 16))
  })

  def posurl = """http:\S+""".r ^^ (url => {

    val m = """http://m\.maps\.yandex\.ru/.*?ll=(?<lonLat>\S+?)&\S*""".r.pattern.matcher(url)
    require(m.matches(), url + "must match" + m)
    val ll = m.group("lonLat").split(",")
    (ll(0).toDouble, ll(1).toDouble)

    KTMap(positionURLKey -> new URL(url), lonlatKey -> (ll(0).toDouble, ll(1).toDouble))
  })

  def movingSatus = """(SPD:\d+km/h \d+|[\d|\.]* ?V=\d+ ?km/h \d+| No GPS data!)""".r ^^ (_ => KTMap.empty)

  def addins = """M=\S+""".r ^^ (_ => KTMap.empty)

  def sensor = """Sensor=\w\/\w \(\w+\)""".r ^^ (_ => KTMap.empty)

  def battery: Parser[KTMap] = regexMatch("""Bat=(?<batval>\S+)-(?<batproc>\S+%)(,I)?""".r) <~ opt("""(Ex_Batt=\S+)""".r) ^^
    (m => Seq(m.gopt("batval").map(batValueKey -> _), m.gopt("batproc").map(batPercentageKey -> _)).flatten.reduce((_: KTMap) ++ (_: KTMap)))

  def temperature = """T=\S+C""".r ^^ (_ => KTMap.empty)

  def t1t2 = """T1: \d\d-\d\d-\d\d\d\d \d\d:\d\d.* T2(: \d\d-\d\d-\d\d\d\d \d\d:\d\d,?)?""".r ^^ (_ => KTMap.empty)

  def mode = """Mode:\w+""".r ^^ (_ => KTMap.empty)

  def sms = """#\d+(SMS)?""".r ^^ (_ => KTMap.empty)

  def tail = """\s*""".r ^^ (_ => KTMap.empty)


  def apply(input: String): ParseResult[MatchResult] = parseAll(fullSms, input.replaceAll("§", "_"))


}
