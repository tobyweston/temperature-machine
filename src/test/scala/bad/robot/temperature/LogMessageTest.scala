package bad.robot.temperature

import java.time.Instant

import org.specs2.mutable.Specification
import org.specs2.matcher.DisjunctionMatchers._

class LogMessageTest extends Specification {

  "Encode Json" >> {
    encode(LogMessage(Instant.parse("2018-01-01T13:00:00.000Z"), "main", "ERROR", "I'm a teapot")).spaces2ps must_==
      """{
        |  "time" : "2018-01-01T13:00:00Z",
        |  "thread" : "main",
        |  "level" : "ERROR",
        |  "message" : "I'm a teapot"
        |}""".stripMargin
  }

  "Decode Json (remember this will need to match the UI rendering method)" >> {
    val json = """{
                 |  "time" : "2018-01-01T13:00:00Z",
                 |  "thread" : "main",
                 |  "level" : "ERROR",
                 |  "message" : "I'm a teapot"
                 |}""".stripMargin
    val expected = LogMessage(Instant.parse("2018-01-01T13:00:00.000Z"), "main", "ERROR", "I'm a teapot")
    decodeAsDisjunction[LogMessage](json) must be_\/-(expected)
  }

}
