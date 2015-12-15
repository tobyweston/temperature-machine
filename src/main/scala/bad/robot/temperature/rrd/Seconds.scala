package bad.robot.temperature.rrd

import java.util.Date

import scala.concurrent.duration.Duration

object Seconds {
  implicit def dateToSeconds(date: Date): Seconds = Seconds(timeInSeconds(date))
  implicit def secondsToLong(seconds: Seconds): Long = seconds.value
  implicit def longToSeconds(seconds: Long): Seconds = Seconds(seconds)
  implicit def durationToSeconds(duration: Duration): Seconds = Seconds(duration.toSeconds)
  implicit def secondsToDuration(seconds: Seconds): Duration = Duration(seconds.value, "seconds")
}

case class Seconds(value: Long) {
  def +(other: Seconds) = Seconds(value + other.value)
  def -(other: Seconds) = Seconds(value - other.value)

  override def toString: String = value + " seconds"
}
