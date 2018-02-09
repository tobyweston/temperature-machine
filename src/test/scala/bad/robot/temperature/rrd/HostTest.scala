package bad.robot.temperature.rrd

import java.net.InetAddress

import bad.robot.temperature._
import org.specs2.matcher.DisjunctionMatchers.be_\/-
import org.specs2.mutable.Specification

class HostTest extends Specification {

  "Trims to max 20 characters (including the 'sensor-n' postfix)" >> {
    Host("cheetah.local", None) must_== Host("cheetah.loc", None) // aka the host name is equal
    "cheetah.loc-sensor-1".length must_== 20
  }

  "Doesn't trim" >> {
    Host("kitchen", None) must_== Host("kitchen", None)
  }

  "Local host" >> {
    Host.local.name must_== InetAddress.getLocalHost.getHostName.take(11)
  }

  "Encode Json" >> {
    encode(Host("local", None)).spaces2ps must_==
      """{
        |  "name" : "local",
        |  "utcOffset" : null
        |}""".stripMargin

    encode(Host("local", Some("+03:00"))).spaces2ps must_==
      """{
        |  "name" : "local",
        |  "utcOffset" : "+03:00"
        |}""".stripMargin
  }

  "Decode Json (what happens if a UTC offset isn't supplied?)" >> {
    val json = """{ "name" : "local" }"""
    decodeAsDisjunction[Host](json) must be_\/-(Host("local", utcOffset = None))
  }
  
}
