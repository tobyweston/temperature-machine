package bad.robot.temperature

import java.time.Instant

import org.specs2.mutable.Specification

class LogMessageTest extends Specification {

  "Encode Json" >> {
    encode(LogMessage(Instant.parse("2018-01-01T13:00:00.000Z"), "main", "ERROR", "I'm a teapot")).spaces2ps must_==
      """{
        |  "time" : {
        |    "instant" : "2018-01-01T13:00:00Z"
        |  },
        |  "thread" : "main",
        |  "level" : "ERROR",
        |  "message" : "I'm a teapot"
        |}""".stripMargin
  }

}
