package bad.robot.temperature.server

import bad.robot.temperature._
import org.http4s.dsl._
import org.http4s.server.HttpService

object TemperatureEndpoint {

  def service(sensors: TemperatureReader, writer: TemperatureWriter) = HttpService {
    case GET -> Root / "temperature" => {
      sensors.read.toHttpResponse(temperatures => {
        Ok(f"${average(temperatures).celsius}%.1f °C")
      })
    }

    case request @ PUT -> Root / "temperature" => {
      val json = request.as[String].run
      val result = for {
        measurement <- decode[Measurement](json)
        _           <- writer.write(measurement.temperatures)
      } yield ()
      result.toHttpResponse(_ => NoContent())
    }
  }

  private val average: (List[Temperature]) => Temperature = (temperatures) => {
    temperatures.fold(Temperature(0.0))(_ + _) / temperatures.length
  }

}