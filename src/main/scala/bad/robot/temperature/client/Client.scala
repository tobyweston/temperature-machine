package bad.robot.temperature.client

import java.net.InetAddress

import bad.robot.logging.{error, _}
import bad.robot.temperature.ds18b20.SensorFile
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.rrd.Host
import bad.robot.temperature.rrd.RrdFile._
import bad.robot.temperature.task.IOs._
import cats.implicits._
import cats.effect.IO
import fs2.StreamApp._
import fs2.Stream
import scalaz.{-\/, \/-}

object Client {

  private val clientHttpPort = 11900

  private val client: List[SensorFile] => Stream[IO, ExitCode] = sensors => {
    for {
      _        <- Stream.eval(info(s"Initialising client '${Host.local.name}' (with ${sensors.size} of a maximum of $MaxSensors sensors)..."))
      server   <- Stream.eval(IO(DiscoveryClient.discover))
      _        <- Stream.eval(info(s"Server discovered on ${server.getHostAddress}, monitoring temperatures..."))
      _        <- Stream.eval(record(Host.local, sensors, HttpUpload(server, BlazeHttpClient())))
      exitCode <- ClientsLogHttpServer(clientHttpPort)
      _        <- Stream.eval(info(s"HTTP Server started to serve logs on http://${InetAddress.getLocalHost.getHostAddress}:$clientHttpPort"))
    } yield exitCode
  }

  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] = {
    findSensors match {
      case \/-(sensors) => client(sensors)
      case -\/(cause)   => Stream.eval(error(cause.message)).flatMap(_ => Stream.emit(ExitCode(1)))
    }
  }
}
