package bad.robot.temperature.server

import java.net.InetAddress

import bad.robot.temperature.Log
import bad.robot.temperature.client.HttpUpload
import bad.robot.temperature.ds18b20.SensorFile
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.rrd.Host
import bad.robot.temperature.task.{Tasks, TemperatureMachineThreadFactory}

import scalaz.concurrent.Task

object Server extends App {

  sys.props += ("org.slf4j.simpleLogger.defaultLogLevel" -> "error")

  def discovery = {
    for {
      _        <- Task.delay(Log.info(s"Starting Discovery Server, listening for ${hosts.map(_.name).mkString("'", "', '", "'")}..."))
      listener <- Task.delay(TemperatureMachineThreadFactory("machine-discovery-server").newThread(new DiscoveryServer()).start())
    } yield ()
  }

  def http(implicit monitored: List[Host]): Task[HttpServer] = {
    val port = 11900
    for {
      server <- HttpServer(port, monitored)
      _      <- Task.delay(Log.info(s"HTTP Server started on http://${InetAddress.getLocalHost.getHostAddress}:$port"))
      _      <- server.awaitShutdown()
    } yield server
  }

  def server(sensors: List[SensorFile])(implicit monitored: List[Host]) = {
    for {
      _ <- Task.delay(Log.info("Starting temperature-machine (server mode)..."))
      _ <- Tasks.init(hosts)
      _ <- Task.gatherUnordered(List(
        discovery,
        Tasks.record(Host.local.trim, sensors, HttpUpload(InetAddress.getLocalHost)),
        Tasks.graphing,
        Tasks.exportJson,
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

  findSensorsAndExecute(server(_)(hosts)).leftMap(error => Log.error(error.message))

}
