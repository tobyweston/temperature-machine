package bad.robot.temperature.rrd

import java.net.InetAddress

import bad.robot.temperature._
import org.specs2.mutable.Specification

class HostTest extends Specification {

  "Trims to max 20 characters (including the 'sensor-n' postfix)" >> {
    Host("cheetah.local") must_== Host("cheetah.loc")
    "cheetah.loc-sensor-1".length must_== 20
  }

  "Doesn't trim" >> {
    Host("kitchen") must_== Host("kitchen")
  }

  "Local host" >> {
    Host.local.name must_== InetAddress.getLocalHost.getHostName.take(11)
  }

  "Encode Json" >> {
    encode(Host("local")).spaces2ps must_==
      """{
        |  "name" : "local"
        |}""".stripMargin
  }

}
