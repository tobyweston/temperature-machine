package bad.robot.temperature

import argonaut.EncodeJson
import argonaut._
import Argonaut._

object SensorTemperature {
  implicit def sensorTemperatureEncoder: EncodeJson[SensorTemperature] = {
    EncodeJson((sensor: SensorTemperature) =>
      argonaut.Json(
        "name"        := sensor.name,
        "temperature" := sensor.temperature
      )
    )
  }

  implicit def sensorTemperatureDecoder: DecodeJson[SensorTemperature] = {
    DecodeJson(cursor => for {
      name        <- cursor.get[String]("name")
      temperature <- cursor.get[Temperature]("temperature")
    } yield SensorTemperature(name, temperature))
  }
}

case class SensorTemperature(name: String, temperature: Temperature) {

}
