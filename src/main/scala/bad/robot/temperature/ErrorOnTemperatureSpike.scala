package bad.robot.temperature

import bad.robot.temperature.ErrorOnTemperatureSpike._
import bad.robot.temperature.PercentageDifference.percentageDifference

import scalaz.{-\/, \/}
import scala.collection.concurrent.TrieMap

object ErrorOnTemperatureSpike {

  private val spikePercentage = 30

  /**
    * @param delegate delegate writer
    * @return a [[TemperatureWriter]] that will produce an error (left disjunction) when a spike over [[spikePercentage]]
    *         is detected or pass through to the delegate if the system property `avoid.spikes` is not set.
    */
  def apply(delegate: TemperatureWriter): TemperatureWriter = {
    sys.props.get("avoid.spikes").map(_ => {
      println(s"Temperature spikes greater than $spikePercentage% will not be recorded")
      new ErrorOnTemperatureSpike(delegate)
    }).getOrElse(delegate)
  }
}

/**
  * This isn't atomic in terms of the `get` and `update` calls against the cached values. An value could be retrieved,
  * the `spikeBetween` check made whilst the cache contains an updated value.
  *
  * However, the cache structure is thread safe, so at worst, you may get out of date data.
  *
  * However, as we know that for every host, at most one call will be made every 30 seconds, there is no risk on
  * concurrent access for a particular sensor (the key to the cache).
  */
class ErrorOnTemperatureSpike(delegate: TemperatureWriter) extends TemperatureWriter {

  private val temperatures: TrieMap[String, Temperature] = TrieMap()

  def write(measurement: Measurement): Error \/ Unit = {

    val spiked = measurement.temperatures.flatMap(current => {
      temperatures.get(current.name) match {
        case Some(previous) if spikeBetween(current, previous) => Some(Spike(current.name, previous, current.temperature))
        case _                                                 => None
      }
    })

    if (spiked.nonEmpty) {
      -\/(SensorSpikeError(spiked))
    } else {
      measurement.temperatures.foreach(current => temperatures.update(current.name, current.temperature))
      delegate.write(measurement)
    }
  }

  private def spikeBetween(reading: SensorReading, previous: Temperature) = {
    percentageDifference(previous.celsius, reading.temperature.celsius) >= spikePercentage
  }

}

case class Spike(sensor: String, previous: Temperature, current: Temperature)