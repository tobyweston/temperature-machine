package bad.robot.temperature.server

import java.net.InetAddress

import bad.robot.temperature.rrd.{RrdFile, Host}
import bad.robot.temperature.task.Tasks

import scalaz.concurrent.Task

object Server extends App {

  def discovery = {
    for {
      _        <- Task.delay(println(s"Starting Discovery Server, listening for ${hosts.map(_.name).mkString(", ")}..."))
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

  implicit val numberOfSensors = RrdFile.MaxSensors

  implicit val hosts = args.toList match {
    case Nil => {
      println(
       """|Usage: Server <hosts>
          |Please supply at least one source host, e.g. 'Server bedroom lounge'
          |""".stripMargin)
      sys.exit(-1)
    }
    case hosts => hosts.map(Host.apply)
  }

  val server = for {
    _ <- Task.delay(println("Starting temperature-machine (server mode)..."))
    _ <- Tasks.init(hosts)
    _ <- Task.gatherUnordered(List(
      discovery,
      Tasks.graphing,
      Tasks.exportXml,
      http)
    )
  } yield ()

  server.run
}
