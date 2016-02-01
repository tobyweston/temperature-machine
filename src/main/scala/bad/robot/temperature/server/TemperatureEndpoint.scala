package bad.robot.temperature.server

import bad.robot.temperature.{Temperature, TemperatureReader}
import org.http4s.dsl._
import org.http4s.server.HttpService

object TemperatureEndpoint {

  def service(sensors: TemperatureReader) = HttpService {
    case GET -> Root / "temperature" => {
      sensors.read.toHttpResponse(temperatures => {
        Ok(f"${average(temperatures).celsius}%.1f °C")
      })
    }

    case PUT -> Root / "temperature" => {
      InternalServerError()
    }
  }

  private val average: (List[Temperature]) => Temperature = (temperatures) => {
    temperatures.fold(Temperature(0.0))(_ + _) / temperatures.length
  }

}