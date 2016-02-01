package bad.robot.temperature

import argonaut.Argonaut._
import argonaut._

object Temperature {

  implicit def temperatureEncoder: EncodeJson[Temperature] = {
    EncodeJson((temperature: Temperature) =>
      argonaut.Json(
        "celsius" := temperature.celsius
      )
    )
  }

  implicit def temperatureDecoder: DecodeJson[Temperature] = {
    DecodeJson(cursor => cursor.get[Double]("celsius").map(Temperature.apply))
  }
}

case class Temperature(celsius: Double) {
  def fahrenheit: Double = celsius * 9 / 5 + 32
  def +(other: Temperature) = new Temperature(this.celsius + other.celsius)
  def /(divisor: Int) = new Temperature(this.celsius / divisor)
}

