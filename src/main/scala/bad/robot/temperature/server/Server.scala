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
import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, Timer}
import scalaz.{-\/, \/, \/-}
import scalaz.syntax.either.ToEitherOps

object Server {

  private def discovery(implicit hosts: List[Host]): IO[Unit] = {
    for {
      _        <- info(s"Starting Discovery Server, listening for ${hosts.map(_.name).mkString("'", "', '", "'")}...")
      listener <- IO(TemperatureMachineThreadFactory("machine-discovery-server").newThread(new DiscoveryServer()).start())
    } yield ()
  }

  private def http(temperatures: AllTemperatures, connections: Connections)(implicit hosts: List[Host], F: ConcurrentEffect[IO], timer: Timer[IO], cs: ContextShift[IO]): IO[ExitCode] = {
    val port = 11900
    for {
      server <- HttpServer(port, hosts).build(temperatures, connections)
      _      <- info(s"HTTP Server started on http://${InetAddress.getLocalHost.getHostAddress}:$port")
    } yield server
  }

  private def server(temperatures: AllTemperatures, connections: Connections, sensors: List[SensorFile])(implicit hosts: List[Host], F: ConcurrentEffect[IO], timer: Timer[IO], cs: ContextShift[IO]): IO[ExitCode] = {
    for {
      _        <- info("Starting temperature-machine (server mode)...")
      _        <- init(hosts)
      _        <- discovery
      _        <- gather(temperatures, Rrd(hosts))
      _        <- record(Host.local, sensors, HttpUpload(InetAddress.getLocalHost, BlazeHttpClient()))
      _        <- graphing
      _        <- exportJson
      exitCode <- http(temperatures, connections)
    } yield exitCode
  }

  private def extractHosts(args: List[String]): Error \/ List[Host] = args match {
    case Nil   => CommandLineError().left
    case hosts => hosts.map(host => Host(host)).right
  }

  def apply(args: List[String])(implicit F: ConcurrentEffect[IO], timer: Timer[IO], cs: ContextShift[IO]): IO[ExitCode] = {
    val application = for {
      hosts        <- extractHosts(args)
      sensors      <- findSensors
    } yield server(AllTemperatures(), Connections(), sensors)(hosts, F, timer, cs)

    application match {
      case \/-(server) => server
      case -\/(cause)  => error(cause.message).flatMap(_ => IO(ExitCode.Error))
    }
  }
}
