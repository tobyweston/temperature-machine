package bad.robot.temperature.server

import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import bad.robot.temperature.rrd.{Host, Rrd}
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.middleware.CORS
import org.http4s.server.syntax.ServiceOps
import org.http4s.server.{Server => Http4sServer}

import scalaz.concurrent.Task

object HttpServer {
  def apply(port: Int, monitored: List[Host]): HttpServer = new HttpServer(port, monitored)
}

class HttpServer(port: Int, monitored: List[Host]) {

  def start() = build().run.awaitShutdown()

  def build(): Task[Http4sServer] = BlazeBuilder.bindHttp(port, "0.0.0.0").mountService(services(), "/").start

  def services() = {
    CORS(
      StaticResources.service ||
        TemperatureResources.service ||
        TemperatureEndpoint.service(SensorReader(SensorFile.find()), Rrd(monitored))
    )
  }
}