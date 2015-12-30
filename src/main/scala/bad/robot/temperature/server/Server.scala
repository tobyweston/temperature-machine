package bad.robot.temperature.server

import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.syntax.ServiceOps

object Server extends App {

  val services = StaticResources.service orElse TemperatureResources.service orElse CurrentTemperature.service

  BlazeBuilder
    .bindHttp(11900, "0.0.0.0")
    .mountService(services, "/")
    .run
    .awaitShutdown()
}