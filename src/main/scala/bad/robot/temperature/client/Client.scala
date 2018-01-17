package bad.robot.temperature.client

import java.lang.Math.max
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors.newFixedThreadPool

import bad.robot.logging._
import bad.robot.temperature.ds18b20.SensorFile
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.rrd.Host
import bad.robot.temperature.rrd.RrdFile._
import bad.robot.temperature.server.LogEndpoint
import bad.robot.temperature.task.{IOs, TemperatureMachineThreadFactory}
import cats.effect.IO
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.middleware.CORS
import org.http4s.server.{Server => Http4sServer}

import scala.concurrent.ExecutionContext

object Client extends App {

  private val clientHttpPort = 11900


  private val latch = new CountDownLatch(1)

  private val client: List[SensorFile] => IO[Unit] = sensors => {
    for {
      _      <- IO.pure(Log.info(s"Initialising client '${Host.local.name}' (with ${sensors.size} of a maximum of $MaxSensors sensors)..."))
      server <- IO.pure(DiscoveryClient.discover)
      _      <- IO.pure(Log.info(s"Server discovered on ${server.getHostAddress}, monitoring temperatures..."))
      _      <- IOs.record(Host.local.trim, sensors, HttpUpload(server))
      _      <- ClientsLogHttpServer(clientHttpPort)
      _      <- IO.pure(Log.info(s"HTTP Server started to serve logs on http://${InetAddress.getLocalHost.getHostAddress}:$clientHttpPort"))
      _      <- awaitShutdown()
    } yield ()
  }

  private def awaitShutdown(): IO[Unit] = IO.pure(latch.await())

  findSensorsAndExecute(client).leftMap(error => Log.error(error.message))

}


object ClientsLogHttpServer {
  def apply(port: Int): IO[ClientsLogHttpServer] = IO.pure {
    val server = new ClientsLogHttpServer(port)
    server.build().unsafeRunSync
    server
  }
}

class ClientsLogHttpServer(port: Int) {

  private val DefaultExecutorService: ExecutionContext = {
    ExecutionContext.fromExecutor(
      newFixedThreadPool(max(4, Runtime.getRuntime.availableProcessors), TemperatureMachineThreadFactory("log-server"))
    )
  }

  private def build(): IO[Http4sServer[IO]] = BlazeBuilder[IO]
    .withExecutionContext(DefaultExecutorService)
    .bindHttp(port, "0.0.0.0")
    .mountService(CORS(LogEndpoint()), "/")
    .start

}
