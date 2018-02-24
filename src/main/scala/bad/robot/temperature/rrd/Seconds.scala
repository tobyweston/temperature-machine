package bad.robot.temperature.rrd

import java.time.Instant

import scala.concurrent.duration.Duration

object Seconds {

  def now() = Seconds(millisToSeconds(System.currentTimeMillis()))
  def millisToSeconds(millis: Long) = (millis + 500) / 1000L

  implicit def secondsToLong(seconds: Seconds): Long = seconds.value
  implicit def longToSeconds(seconds: Long): Seconds = Seconds(seconds)
  implicit def durationToSeconds(duration: Duration): Seconds = Seconds(duration.toSeconds)
  implicit def secondsToDuration(seconds: Seconds): Duration = Duration(seconds.value, "seconds")
}

case class Seconds(value: Long) {
  def +(other: Seconds) = Seconds(value + other.value)
  def -(other: Seconds) = Seconds(value - other.value)
  def toInstant: Instant = Instant.ofEpochSecond(value)
  def isAfter(instant: Instant) = toInstant.isAfter(instant)
  def isBefore(instant: Instant) = toInstant.isBefore(instant)

  override def toString: String = value + " seconds"
}
