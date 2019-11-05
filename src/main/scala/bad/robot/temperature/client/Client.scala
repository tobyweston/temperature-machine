package bad.robot.temperature.client

import java.net.InetAddress

import bad.robot.logging.{error, _}
import bad.robot.temperature.ds18b20.SensorFile
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.rrd.Host
import bad.robot.temperature.rrd.RrdFile._
import bad.robot.temperature.task.IOs._
import cats.effect.{ConcurrentEffect, ExitCode, IO, Timer}
import scalaz.{-\/, \/-}

object Client {

  private val clientHttpPort = 11900

  private def client(sensors: List[SensorFile])(implicit F: ConcurrentEffect[IO], timer: Timer[IO]): IO[ExitCode] = {
    for {
      _        <- info(s"Initialising client '${Host.local.name}' (with ${sensors.size} of a maximum of $MaxSensors sensors)...")
      server   <- IO(DiscoveryClient.discover)
      _        <- info(s"Server discovered on ${server.getHostAddress}, monitoring temperatures...")
      _        <- record(Host.local, sensors, HttpUpload(server, BlazeHttpClient()))
      exitCode <- ClientsLogHttpServer(clientHttpPort)
      _        <- info(s"HTTP Server started to serve logs on http://${InetAddress.getLocalHost.getHostAddress}:$clientHttpPort")
    } yield exitCode
  }

  def apply(args: List[String])(implicit F: ConcurrentEffect[IO], timer: Timer[IO]): IO[ExitCode] = {
    findSensors match {
      case \/-(sensors) => client(sensors)
      case -\/(cause)   => error(cause.message).map(_ => ExitCode.Error)
    }
  }
}
