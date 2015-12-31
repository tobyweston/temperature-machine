package bad.robot.temperature.server

import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import org.http4s.dsl._
import org.http4s.server.HttpService

object CurrentTemperature {

  private val sensor = SensorFile.find().head

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