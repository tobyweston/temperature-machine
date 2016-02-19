package bad.robot.temperature.rrd

import bad.robot.temperature.rrd.RpnGenerator.{Max, Min}
import org.specs2.mutable.Specification

class RpnGeneratorTest extends Specification {

  "generate a RPN string to aggregate sensors (Max)" >> {
    RpnGenerator.generateRpn(List("bedroom-1"), Max) must_== "bedroom-1,MAX"
    RpnGenerator.generateRpn(List("bedroom-1", "bedroom-2", "lounge-1"), Max) must_== "bedroom-1,bedroom-2,lounge-1,MAX,MAX"
  }

  "generate a RPN string to aggregate sensors (Min)" >> {
    RpnGenerator.generateRpn(List("bedroom-1"), Min) must_== "bedroom-1,MIN"
    RpnGenerator.generateRpn(List("bedroom-1", "bedroom-2", "lounge-1"), Min) must_== "bedroom-1,bedroom-2,lounge-1,MIN,MIN"
  }

}
