package bad.robot.temperature.server

import org.http4s.server.blaze.BlazeBuilder

object Server extends App {
  BlazeBuilder
    .bindHttp(11900, "0.0.0.0")
    .mountService(StaticResources.service, "/")
    .run
    .awaitShutdown()
}