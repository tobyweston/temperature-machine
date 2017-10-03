package bad.robot.temperature.rrd

import java.time.Instant

import org.specs2.mutable.Specification

class SecondsTest extends Specification {

  "Rounding" >> {
    Seconds.millisToSeconds(499L) must_== 0
    Seconds.millisToSeconds(600L) must_== 1
    Seconds.millisToSeconds(1200L) must_== 1
    Seconds.millisToSeconds(1500L) must_== 2
  }

  "To instant" >> {
    Seconds(1492366490).toInstant must_== Instant.parse("2017-04-16T18:14:50Z")
  }

}
