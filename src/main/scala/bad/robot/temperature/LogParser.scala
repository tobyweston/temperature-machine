package bad.robot.temperature

import java.time.Instant
import java.time.format.DateTimeFormatterBuilder

import scala.util.parsing.combinator.RegexParsers
import scalaz.{-\/, \/-}

object LogParser extends RegexParsers {

  implicit class ParserResultOps(result: LogParser.ParseResult[LogMessage]) {
    def toDisjunction() = result match {
      case Success(log, _)  => \/-(log)
      case error: NoSuccess => -\/(ParseError(error.toString))
    }
  }
  
  private val utcFormatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd HH:mm:ss:SSSZ").toFormatter

  private def digits2: Parser[Int] ="""\d{2}""".r ^^ {_.toInt}
  private def digits3: Parser[Int] ="""\d{3}""".r ^^ {_.toInt}
  private def digits4: Parser[Int] ="""\d{4}""".r ^^ {_.toInt}
  private def thread: Parser[String] = """\[(.*?)\]""".r
  private def level: Parser[String] = """INFO|WARN|ERROR|TRACE|DEBUG""".r
  private def words: Parser[String] = """(?s:.*)""".r
  
  def instant: Parser[Instant] = {
    val constituents = digits4 ~ ("-" ~> digits2) ~ ("-" ~> digits2) ~ digits2 ~ (":" ~> digits2) ~ (":" ~> digits2) ~ (":" ~> digits3) ~ ("+" ~> digits4)

    constituents ^^ {
      case year ~ month ~ day ~ hour ~ mins ~ secs ~ millis ~ offset => {
        val string = f"$year-$month%02d-$day%02d $hour%02d:$mins%02d:$secs%02d:$millis%03d+$offset%04d"
        Instant.from(utcFormatter.parse(string))
      }
    }
  }

  def log = {
    instant ~ thread ~ level ~ words ^^ {
      case time ~ thread ~ level ~ message => LogMessage(time, thread, level, message)
    }
  }
}


