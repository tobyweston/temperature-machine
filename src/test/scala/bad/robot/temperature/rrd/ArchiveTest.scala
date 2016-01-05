package bad.robot.temperature.rrd

import org.specs2.mutable.Specification

import scala.concurrent.duration.Duration

class ArchiveTest extends Specification {

  "Some typical RRA values" >> {
    val frequency = Duration(30, "seconds")

    val daily = Archive.apply(aDay, frequency, frequency)
    val weekHourlyAvg = Archive.apply(aWeek, frequency, anHour)
    val monthTwoHourlyAvg = Archive.apply(aMonth, frequency, anHour * 2)

    daily             must_== Archive(1, 2880)
    weekHourlyAvg     must_== Archive(120, 168)
    monthTwoHourlyAvg must_== Archive(240, 360)
  }

  "Some typical RRA values (1 min frequency)" >> {
    val frequency = Duration(1, "minute")

    val daily = Archive.apply(aDay, frequency, frequency)
    val weekHourlyAvg = Archive.apply(aWeek, frequency, anHour)
    val monthTwoHourlyAvg = Archive.apply(aMonth, frequency, anHour * 2)

    daily             must_== Archive(1, 1440)
    weekHourlyAvg     must_== Archive(60, 168)
    monthTwoHourlyAvg must_== Archive(120, 360)
  }
}
