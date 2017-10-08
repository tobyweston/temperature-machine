package bad.robot.temperature

import bad.robot.temperature.IgnoreTemperatureSpikes._

import scala.collection.concurrent.TrieMap

object IgnoreTemperatureSpikes {
  type SensorName = String

  private val spikePercentage = 30

  // negative numbers would be a decrease, which we'll ignore (use Math.abs if we change our mind later)
  def percentageIncrease(oldValue: Double, newValue: Double): Double = (newValue - oldValue) / oldValue * 100
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
class IgnoreTemperatureSpikes(delegate: TemperatureWriter) extends TemperatureWriter {

  private val temperatures: TrieMap[SensorName, Temperature] = TrieMap()

  def write(measurement: Measurement) = {

    val readings = measurement.temperatures.map(current => {
      temperatures.get(current.name) match {
        case Some(previous) if spikeBetween(current, previous) => current.copy(temperature = previous)
        case _                                                 => temperatures.update(current.name, current.temperature); current
      }
    })

    delegate.write(measurement.copy(temperatures = readings))
  }

  private def spikeBetween(reading: SensorReading, previous: Temperature) = {
    percentageIncrease(previous.celsius, reading.temperature.celsius) >= spikePercentage
  }

}
