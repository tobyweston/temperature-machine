package bad.robot.temperature.server

import java.net.InetAddress

import bad.robot.temperature.client.HttpUpload
import bad.robot.temperature.ds18b20.SensorFile
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.rrd.{RrdFile, Host, Rrd}
import bad.robot.temperature.task.Tasks

import scalaz.concurrent.Task

object Server extends App {

  def discovery = {
    for {
      _        <- Task.delay(println(s"Starting Discovery Server, listening for ${hosts.map(_.name).mkString("'", "', '", "'")}..."))
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

  def server(sensors: List[SensorFile])(implicit monitored: List[Host]) = {
    for {
      _ <- Task.delay(println("Starting temperature-machine (server mode)..."))
      _ <- Tasks.init(hosts)
      _ <- Task.gatherUnordered(List(
        discovery,
        Tasks.record(Host.local.trim, sensors, HttpUpload(InetAddress.getLocalHost)),
        Tasks.graphing,
        Tasks.exportXml,
        http
      ))
    } yield ()
  }

  def quit = {
    println(
      """|Usage: Server <hosts>
        |Please supply at least one source host, e.g. 'Server bedroom lounge'
        |""".stripMargin)
    sys.exit(-1)
  }



   val hosts = args.toList match {
    case Nil   => quit
    case hosts => hosts.map(host => Host.apply(host).trim)
  }

  findSensorsAndExecute(server(_)(hosts))
}
