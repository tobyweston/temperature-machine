package bad.robot.temperature.server

import java.net.InetAddress

import bad.robot.logging._
import bad.robot.temperature.Error
import bad.robot.temperature.CommandLineError
import bad.robot.temperature.client.{BlazeHttpClient, HttpUpload}
import bad.robot.temperature.ds18b20.SensorFile
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.rrd.{Host, Rrd}
import bad.robot.temperature.task.IOs._
import bad.robot.temperature.task.TemperatureMachineThreadFactory
import cats.effect.IO
import fs2.Stream
import fs2.StreamApp
import fs2.StreamApp.ExitCode
import scalaz.\/
import scalaz.syntax.either.ToEitherOps

object Server extends StreamApp[IO] {

  def discovery(implicit hosts: List[Host]): IO[Unit] = {
    for {
      _        <- info(s"Starting Discovery Server, listening for ${hosts.map(_.name).mkString("'", "', '", "'")}...")
      listener <- IO(TemperatureMachineThreadFactory("machine-discovery-server").newThread(new DiscoveryServer()).start())
    } yield ()
  }

  def http(temperatures: AllTemperatures, connections: Connections)(implicit hosts: List[Host]): Stream[IO, ExitCode] = {
    val port = 11900
    for {
      server <- HttpServer(port, hosts, temperatures, connections)
      _      <- Stream.eval(info(s"HTTP Server started on http://${InetAddress.getLocalHost.getHostAddress}:$port"))
    } yield server
  }

  def server(temperatures: AllTemperatures, connections: Connections, sensors: List[SensorFile])(implicit hosts: List[Host]): Stream[IO, ExitCode] = {
    for {
      _        <- Stream.eval(info("Starting temperature-machine (server mode)..."))
      _        <- Stream.eval(init(hosts))
      _        <- Stream.eval(discovery)
      _        <- Stream.eval(gather(temperatures, Rrd(hosts)))
      _        <- Stream.eval(record(Host.local, sensors, HttpUpload(InetAddress.getLocalHost, BlazeHttpClient())))
      _        <- Stream.eval(graphing)
      _        <- Stream.eval(exportJson)
      exitCode <- http(temperatures, connections)
    } yield exitCode
  }

  override def stream(args: List[String], shutdown: IO[Unit]): Stream[IO, ExitCode] = {
    val application = for {
      hosts        <- extractHosts(args)
      sensors      <- findSensors
      temperatures  = AllTemperatures()
      connections   = Connections()
    } yield server(temperatures, connections, sensors)(hosts)
    
    application.leftMap(error => Log.error(error.message)).getOrElse(Stream.emit(ExitCode(1)))
  }

  private def extractHosts(args: List[String]): Error \/ List[Host] = args match {
    case Nil   => CommandLineError().left
    case hosts => hosts.map(host => Host(host)).right
  }
}
