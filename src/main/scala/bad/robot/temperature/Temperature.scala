package bad.robot.temperature

import io.circe._

object Temperature {

  implicit def encoder: Encoder[Temperature] = new Encoder[Temperature] {
    def apply(temperature: Temperature): Json = Json.obj(
      ("celsius", Json.fromDoubleOrNull(temperature.celsius))
    )
  }

  implicit def decoder: Decoder[Temperature] = new Decoder[Temperature] {
    def apply(cursor: HCursor): Decoder.Result[Temperature] =
      cursor.get[Double]("celsius").map(Temperature.apply)
  }

}

case class Temperature(celsius: Double) {
  def fahrenheit: Double = celsius * 9 / 5 + 32
  def +(other: Temperature) = new Temperature(this.celsius + other.celsius)
  def /(divisor: Int) = new Temperature(this.celsius / divisor)

  def asCelsius = f"$celsius%.1f °C"
  def asFahrenheit = f"$fahrenheit%.1f °F"
}

