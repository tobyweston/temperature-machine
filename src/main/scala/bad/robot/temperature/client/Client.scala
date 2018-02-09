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
import bad.robot.temperature.server.{LogEndpoint, VersionEndpoint}
import bad.robot.temperature.task.IOs._
import bad.robot.temperature.task.{TemperatureMachineThreadFactory}
import cats.implicits._
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
      _      <- info(s"Initialising client '${Host.local.name}' (with ${sensors.size} of a maximum of $MaxSensors sensors)...")
      server <- IO(DiscoveryClient.discover)
      _      <- info(s"Server discovered on ${server.getHostAddress}, monitoring temperatures...")
      _      <- record(Host.local, sensors, HttpUpload(server, BlazeHttpClient()))
      _      <- ClientsLogHttpServer(clientHttpPort)
      _      <- info(s"HTTP Server started to serve logs on http://${InetAddress.getLocalHost.getHostAddress}:$clientHttpPort")
      _      <- awaitShutdown()
    } yield ()
  }

  private def awaitShutdown(): IO[Unit] = IO(latch.await())

  findSensorsAndExecute(client).leftMap(error => Log.error(error.message))

}


object ClientsLogHttpServer {
  def apply(port: Int): IO[Http4sServer[IO]] = {
    val server = new ClientsLogHttpServer(port)
    server.build()
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
    .mountService(
      CORS(
        LogEndpoint() <+>
        VersionEndpoint()
      ), "/")
    .start

}
