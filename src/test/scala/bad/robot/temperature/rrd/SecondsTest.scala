package bad.robot.temperature.rrd

import org.specs2.mutable.Specification

import scala.language.postfixOps

class SecondsTest extends Specification {

  "Rounding" >> {
    Seconds.millisToSeconds(499L) must_== 0
    Seconds.millisToSeconds(600L) must_== 1
    Seconds.millisToSeconds(1200L) must_== 1
    Seconds.millisToSeconds(1500L) must_== 2
  }

}
