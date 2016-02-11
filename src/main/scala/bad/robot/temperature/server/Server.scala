package bad.robot.temperature.server

import java.net.InetAddress

import bad.robot.temperature.rrd.Host

import scalaz.concurrent.Task

object Server extends App {

  def discovery = {
    for {
      _        <- Task.delay(println(s"Starting Discovery Server, listening for ${hosts.map(_.name).mkString}..."))
      listener <- Task.delay(new Thread(new DiscoveryServer(), "temperature-machine-discovery-server").start())
    } yield ()
  }

  def http(implicit monitored: List[Host]) = {
    val port = 11900
    for {
      http <- HttpServer(port, monitored).build()
      _    <- Task.delay(println(s"HTTP Server started on http://${InetAddress.getLocalHost.getHostAddress}:$port"))
      _    <- Task.delay(http.awaitShutdown())
    } yield http
  }


  implicit val hosts = args.toList match {
    case Nil => sys.error("Usage: Server <host>\nPlease supply at least one source host, e.g. 'bedroom'")
    case list => list.map(Host.apply)
  }

  val server = for {
    _ <- Task.delay(println("Starting temperature-machine (server mode)..."))
         // TODO rrd init
    _ <- Task.gatherUnordered(List(discovery, http))
         // TODO add graphing / XML export
  } yield ()

  server.run
}
