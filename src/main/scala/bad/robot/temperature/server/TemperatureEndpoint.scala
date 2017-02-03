package bad.robot.temperature.server

import argonaut.EncodeJson
import bad.robot.temperature._
import bad.robot.temperature.rrd.Host
import org.http4s.dsl._
import org.http4s.server.HttpService

object TemperatureEndpoint {

  implicit def hostAndMeasurementEncoder: EncodeJson[Map[Host, Measurement]] = {
    import argonaut._
    import Argonaut._

    EncodeJson((measurements: Map[Host, Measurement]) =>
      argonaut.Json(
        "measurements" := measurements.values
      )
    )
  }

  private var current: Map[Host, Measurement] = Map()

  def service(sensors: TemperatureReader, writer: TemperatureWriter) = HttpService {
    case GET -> Root / "temperature" => {
      sensors.read.toHttpResponse(temperatures => {
        Ok(f"${temperatures.average.temperature.celsius}%.1f °C")
      })
    }

    case GET -> Root / "temperatures" / "average" => {
      val average = current.map { case (host, measurement) => {
        host -> measurement.copy(temperatures = List(measurement.temperatures.average))
      }}
      Ok(encode(average).spaces2)
    }

    case GET -> Root / "temperatures"  => {
      Ok(encode(current).spaces2)
    }

    case DELETE -> Root / "temperatures" => {
      current = Map[Host, Measurement]()
      NoContent()
    }

    case request @ PUT -> Root / "temperature" => {
      val json = request.as[String].run
      val result = for {
        measurement <- decode[Measurement](json)
        _           <- writer.write(measurement)
      } yield measurement
      result.toHttpResponse(success => {
        current = current + (success.host -> success)
        NoContent()
      })
    }
  }

}