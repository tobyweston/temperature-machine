package bad.robot.temperature

import java.time.Instant
import java.time.ZoneId.systemDefault
import java.time.format.DateTimeFormatterBuilder

import scala.util.parsing.combinator.RegexParsers

object LogParser extends RegexParsers {

  private val formatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd HH:mm:ss:SSS").toFormatter.withZone(systemDefault())

  private def digits2: Parser[Int] ="""\d{2}""".r ^^ {_.toInt}
  private def digits3: Parser[Int] ="""\d{3}""".r ^^ {_.toInt}
  private def digits4: Parser[Int] ="""\d{4}""".r ^^ {_.toInt}
  private def thread: Parser[String] = """\[(.*?)\]""".r
  private def level: Parser[String] = """INFO|WARN|ERROR|TRACE|DEBUG""".r
  private def words: Parser[String] = """(.|\n)*""".r
    private def instant: Parser[Instant] = {
    val constituents = digits4 ~ ("-" ~> digits2) ~ ("-" ~> digits2) ~ digits2 ~ (":" ~> digits2) ~ (":" ~> digits2) ~ (":" ~> digits3)

    constituents ^^ {
      case year ~ month ~ day ~ hour ~ mins ~ secs ~ millis => {
        val string = f"$year-$month%02d-$day%02d $hour%02d:$mins%02d:$secs%02d:$millis%03d"
        Instant.from(formatter.parse(string))
      }
    }
  }

  def log = {
    instant ~ thread ~ level ~ words ^^ {
      case time ~ thread ~ level ~ message => LogMessage(time, thread, level, message)
    }
  }
}


