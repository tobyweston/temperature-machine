package bad.robot.temperature.server

import bad.robot.temperature.Measurements._
import bad.robot.temperature.ds18b20.SensorReader
import org.http4s.dsl._
import org.http4s.server.HttpService

object CurrentTemperature {

  def service = HttpService {

    case GET -> Root / "temperature" => {
      SensorReader(sensor).read.fold(error => {
        InternalServerError(error.message)
      }, temperature => {
        Ok(s"${temperature.celsius} °C")
      })
    }
  }

}