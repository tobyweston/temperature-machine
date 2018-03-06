package bad.robot.temperature.client

import java.lang.Math.max
import java.net.InetAddress
import java.util.concurrent.Executors.newFixedThreadPool

import bad.robot.logging._
import bad.robot.temperature.ds18b20.SensorFile
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.rrd.Host
import bad.robot.temperature.rrd.RrdFile._
import bad.robot.temperature.server.{LogEndpoint, VersionEndpoint}
import bad.robot.temperature.task.IOs._
import bad.robot.temperature.task.TemperatureMachineThreadFactory
import cats.implicits._
import cats.effect.IO
import fs2.StreamApp._
import fs2.{Stream, StreamApp}
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.middleware.CORS

import scala.concurrent.ExecutionContext
import scalaz.{-\/, \/-}

object Client extends StreamApp[IO] {

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

  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] = {
    findSensors match {
      case \/-(sensors) => client(sensors)
      case -\/(error)   => Log.error(error.message); Stream.emit(ExitCode(1))
    }
  }
}


object ClientsLogHttpServer {
  def apply(port: Int): Stream[IO, ExitCode] = new ClientsLogHttpServer(port).build()
}

class ClientsLogHttpServer(port: Int) {

  private val DefaultExecutorService: ExecutionContext = {
    ExecutionContext.fromExecutor(
      newFixedThreadPool(max(4, Runtime.getRuntime.availableProcessors), TemperatureMachineThreadFactory("log-server"))
    )
  }

  private def build(): Stream[IO, ExitCode] = {
    import scala.concurrent.ExecutionContext.Implicits.global // todo replace with explicit one

    BlazeBuilder[IO]
      .withExecutionContext(DefaultExecutorService)
      .bindHttp(port, "0.0.0.0")
      .mountService(
        CORS(
          LogEndpoint() <+>
            VersionEndpoint()
        ), "/")
      .serve
  }

}
