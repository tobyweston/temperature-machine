package bad.robot.temperature.server

import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import bad.robot.temperature.server.Server._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.syntax.ServiceOps

object Server {
  val services =
    StaticResources.service orElse
    TemperatureResources.service orElse
    CurrentAverageTemperature.service(SensorReader(SensorFile.find()))
}

case class Server(port: Int) {

  def start() = {
    BlazeBuilder
      .bindHttp(port, "0.0.0.0")
      .mountService(services, "/")
      .run
      .awaitShutdown()
  }
}