package bad.robot.temperature.server

import java.net.InetAddress

import bad.robot.logging._
import bad.robot.temperature.client.HttpUpload
import bad.robot.temperature.ds18b20.SensorFile
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.rrd.Host
import bad.robot.temperature.task.IOs._
import bad.robot.temperature.task.TemperatureMachineThreadFactory
import cats.effect.IO

object Server extends App {

  def discovery: IO[Unit] = {
    for {
      _        <- IO.pure(Log.info(s"Starting Discovery Server, listening for ${hosts.map(_.name).mkString("'", "', '", "'")}..."))
      listener <- IO.pure(TemperatureMachineThreadFactory("machine-discovery-server").newThread(new DiscoveryServer()).start())
    } yield ()
  }

  def http(implicit monitored: List[Host]): IO[HttpServer] = {
    val port = 11900
    for {
      server <- HttpServer(port, monitored)
      _      <- IO.pure(Log.info(s"HTTP Server started on http://${InetAddress.getLocalHost.getHostAddress}:$port"))
      _      <- server.awaitShutdown()
    } yield server
  }

  def server(sensors: List[SensorFile])(implicit monitored: List[Host]) = {
    for {
      _ <- IO.pure(Log.info("Starting temperature-machine (server mode)..."))
      _ <- init(hosts)
      _ <- discovery
      _ <- record(Host.local.trim, sensors, HttpUpload(InetAddress.getLocalHost))
      _ <- graphing
      _ <- exportJson
      _ <- http
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
