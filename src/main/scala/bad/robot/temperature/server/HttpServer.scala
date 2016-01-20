package bad.robot.temperature.server

import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import bad.robot.temperature.server.HttpServer._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.syntax.ServiceOps

object HttpServer {
  val services =
    StaticResources.service orElse
    TemperatureResources.service orElse
    CurrentAverageTemperature.service(SensorReader(SensorFile.find()))
}

case class HttpServer(port: Int) {

  def start() = {
    BlazeBuilder
      .bindHttp(port, "0.0.0.0")
      .mountService(services, "/")
      .run
      .awaitShutdown()
  }
}