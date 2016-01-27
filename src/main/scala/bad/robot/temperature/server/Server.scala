package bad.robot.temperature.server

import java.net.InetAddress

import scalaz.concurrent.Task

object Server extends App {

  val Port = 11900

  val discovery = {
    for {
      _        <- Task.delay(println("Starting Discovery Server..."))
      listener <- Task.delay(new Thread(new DiscoveryServer(), "temperature-machine-discovery-server").start())
    } yield ()
  }

  val http = {
    for {
      http <- HttpServer(Port).build()
      _    <- Task.delay(println(s"HTTP Server started on http://${InetAddress.getLocalHost.getHostAddress}:$Port"))
      _    <- Task.delay(http.awaitShutdown())
    } yield http
  }

  val server = for {
    _ <- Task.delay(println("Starting temperature-machine (Server Mode)..."))
    _ <- Task.gatherUnordered(List(discovery, http))
  } yield ()

  server.run
}
