package bad.robot.temperature.rrd

import java.util.Date

object Seconds {
  implicit def dateToSeconds(date: Date): Seconds = Seconds(timeInSeconds(date))
  implicit def secondsToLong(seconds: Seconds): Long = seconds.value
  implicit def longToSeconds(seconds: Long): Seconds = Seconds(seconds)
}

case class Seconds(value: Long)
