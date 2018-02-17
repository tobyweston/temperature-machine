package bad.robot.temperature.server

import java.time.Clock
import java.time.temporal.ChronoUnit.{MINUTES => minutes}
import java.time.temporal.TemporalUnit

import bad.robot.temperature.Measurement
import bad.robot.temperature.rrd.Host

import scala.collection.concurrent.TrieMap

case class CurrentTemperatures(clock: Clock) {

  private val temperatures: TrieMap[Host, Measurement] = TrieMap()

  private implicit val implicitClock = clock
  
  
  def average: Map[Host, Measurement] = {
    temperatures.filter(within(5, minutes)).map { case (host, measurement) => {
      host -> measurement.copy(temperatures = List(measurement.temperatures.average))
    }}.toMap
  }
  
  def all: Map[Host, Measurement] = temperatures.filter(within(5, minutes)).toMap
  
  def updateWith(measurement: Measurement) = temperatures.put(measurement.host, measurement)
  
  def clear() = temperatures.clear()


  private def within(amount: Long, unit: TemporalUnit)(implicit clock: Clock): ((Host, Measurement)) => Boolean = {
    case (_, measurement) => measurement.time.isAfter(clock.instant().minus(amount, unit))
  }
}
