package bad.robot.temperature.server

import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import bad.robot.temperature.server.HttpServer._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.syntax.ServiceOps
import org.http4s.server.{Server => Http4sServer}

import scalaz.concurrent.Task

object HttpServer {
  val services =
    StaticResources.service orElse
    TemperatureResources.service orElse
    TemperatureEndpoint.service(SensorReader(SensorFile.find()))
}

case class HttpServer(port: Int) {

  def start() = build().run.awaitShutdown()

  def build(): Task[Http4sServer] = BlazeBuilder.bindHttp(port, "0.0.0.0").mountService(services, "/").start
}