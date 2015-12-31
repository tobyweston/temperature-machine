package bad.robot.temperature.server

import bad.robot.temperature.server.Server._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.syntax.ServiceOps

object Server {
  val services = StaticResources.service orElse TemperatureResources.service orElse CurrentTemperature.service
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