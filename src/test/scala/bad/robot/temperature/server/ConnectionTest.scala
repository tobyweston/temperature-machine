package bad.robot.temperature.server

import bad.robot.temperature._
import bad.robot.temperature.rrd.Host
import org.specs2.mutable.Specification

class ConnectionTest extends Specification {

  "Encode Json" >> {
    encode(Connection(Host("box.local", Some("Z")), IpAddress("127.0.0.1"))).spaces2ps must_==
      """{
        |  "host" : {
        |    "name" : "box.local",
        |    "utcOffset" : "Z"
        |  },
        |  "ip" : {
        |    "value" : "127.0.0.1"
        |  }
        |}""".stripMargin
  }

}
