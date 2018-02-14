package bad.robot.temperature

import bad.robot.logging._
import bad.robot.temperature.ErrorOnTemperatureSpike.DefaultBarrier

import scala.collection.concurrent.TrieMap
import scala.util.{Failure, Success, Try}
import scalaz.{-\/, \/}

object ErrorOnTemperatureSpike {

  private val DefaultBarrier = Barrier(5)

  /**
    * @param delegate delegate writer
    * @return a [[TemperatureWriter]] that will produce an error (left disjunction) when a spike over n degrees
    *         is detected or pass through to the delegate if the system property `avoid.spikes` is not set.
    *         
    *         n is configured via a system property `avoid.spikes` and must be greater than 1 and less than 100
    */
  def apply(delegate: TemperatureWriter): TemperatureWriter = {
    sys.props.get("avoid.spikes").map(value => {
      val barrier = toBarrier(value)
      Log.info(s"Temperature spikes greater than +/-${barrier.degrees} °C will not be recorded")
      new ErrorOnTemperatureSpike(delegate, barrier)
    }).getOrElse(delegate)
  }
  
  private def toBarrier(string: String) = Try(string.toInt) match {
    case Success(value) if value >= 1 && value <= 100 => Barrier(value)
    case Failure(_)                                   => DefaultBarrier
    case _                                            => DefaultBarrier
  }
  
}

/**
  * This isn't atomic in terms of the `get` and `update` calls against the cached values. A value could be retrieved,
  * the `spikeBetween` check made whilst the cache contains an updated value.
  *
  * However, the cache structure is thread safe, so at worst, you may get out of date data.
  *
  * However, as we know that for every host, at most one call will be made every 30 seconds, there is no risk on
  * concurrent access for a particular sensor (the key to the cache).
  */
class ErrorOnTemperatureSpike(delegate: TemperatureWriter, barrier: Barrier = DefaultBarrier) extends TemperatureWriter {

  private val temperatures: TrieMap[String, Temperature] = TrieMap()

  def write(measurement: Measurement): Error \/ Unit = {

    val spiked = measurement.temperatures.flatMap(current => {
      temperatures.get(current.name) match {
        case Some(previous) if breached(current, previous) => Some(Spike(current.name, previous, current.temperature))
        case _                                             => None
      }
    })

    if (spiked.nonEmpty) {
      -\/(SensorSpikeError(spiked))
    } else {
      measurement.temperatures.foreach(current => temperatures.update(current.name, current.temperature))
      delegate.write(measurement)
    }
  }

  private def breached(reading: SensorReading, previous: Temperature) = {
    barrier.breached(previous, reading.temperature)
  }

}

case class Spike(sensor: String, previous: Temperature, current: Temperature)