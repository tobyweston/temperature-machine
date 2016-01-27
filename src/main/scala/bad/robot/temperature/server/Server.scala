package bad.robot.temperature.server

import java.net.InetAddress

import scalaz.concurrent.Task

object Server extends App {

  def discovery = {
    for {
      _        <- Task.delay(println("Starting Discovery Server..."))
      listener <- Task.delay(new Thread(new DiscoveryServer(), "temperature-machine-discovery-server").start())
    } yield ()
  }

  def http = {
    val port = 11900
    for {
      http <- HttpServer(port).build()
      _    <- Task.delay(println(s"HTTP Server started on http://${InetAddress.getLocalHost.getHostAddress}:$port"))
      _    <- Task.delay(http.awaitShutdown())
    } yield http
  }

  val server = for {
    _ <- Task.delay(println("Starting temperature-machine (Server Mode)..."))
    _ <- Task.gatherUnordered(List(discovery, http))
  } yield ()

  server.run
}
