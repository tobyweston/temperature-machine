package bad.robot.temperature

import argonaut.EncodeJson
import argonaut._
import Argonaut._

object SensorReading {
  implicit def jsonEncoder: EncodeJson[SensorReading] = {
    EncodeJson((sensor: SensorReading) =>
      argonaut.Json(
        "name"        := sensor.name,
        "temperature" := sensor.temperature
      )
    )
  }

  implicit def jsonDecoder: DecodeJson[SensorReading] = {
    DecodeJson(cursor => for {
      name        <- cursor.get[String]("name")
      temperature <- cursor.get[Temperature]("temperature")
    } yield SensorReading(name, temperature))
  }

  implicit class ListSensorTemperatureOps(temperatures: List[SensorReading]) {
    def average: SensorReading = temperatures match {
      case Nil           => SensorReading("Unknown", Temperature(0.0))
      case sensor :: Nil => SensorReading(sensor.name, sensor.temperature)
      case _             => SensorReading("Average", temperatures.map(_.temperature).reduce(_ + _) / temperatures.length)
    }
  }
}

case class SensorReading(name: String, temperature: Temperature)
