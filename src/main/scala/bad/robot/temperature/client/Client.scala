package bad.robot.temperature.client

import java.lang.Math.max
import java.net.InetAddress
import java.util.concurrent.Executors.newFixedThreadPool
import java.util.concurrent.{CountDownLatch, ExecutorService}

import bad.robot.temperature.Log
import bad.robot.temperature.ds18b20.SensorFile
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.rrd.Host
import bad.robot.temperature.rrd.RrdFile._
import bad.robot.temperature.server.LogEndpoint
import bad.robot.temperature.task.{Tasks, TemperatureMachineThreadFactory}
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.{Server => Http4sServer}

import scalaz.concurrent.Task

object Client extends App {

  private val clientHttpPort = 11900


  private val latch = new CountDownLatch(1)

  private val client: List[SensorFile] => Task[Unit] = sensors => {
    for {
      _      <- Task.delay(Log.info(s"Initialising client '${Host.local.name}' (with ${sensors.size} of a maximum of $MaxSensors sensors)..."))
      server <- Task.delay(DiscoveryClient.discover)
      _      <- Task.delay(Log.info(s"Server discovered on ${server.getHostAddress}, monitoring temperatures..."))
      _      <- Tasks.record(Host.local.trim, sensors, HttpUpload(server))
      _      <- ClientsLogHttpServer(clientHttpPort)
      _      <- Task.delay(Log.info(s"HTTP Server started to serve logs on http://${InetAddress.getLocalHost.getHostAddress}:$clientHttpPort"))
      _      <- awaitShutdown()
    } yield ()
  }

  private def awaitShutdown(): Task[Unit] = Task.delay(latch.await())

  findSensorsAndExecute(client).leftMap(error => Log.error(error.message))

}


object ClientsLogHttpServer {
  def apply(port: Int): Task[ClientsLogHttpServer] = Task.delay {
    val server = new ClientsLogHttpServer(port)
    server.build().unsafePerformSync
    server
  }
}

class ClientsLogHttpServer(port: Int) {

  private val DefaultExecutorService: ExecutorService = {
    newFixedThreadPool(max(4, Runtime.getRuntime.availableProcessors), TemperatureMachineThreadFactory("log-server"))
  }

  private def build(): Task[Http4sServer] = BlazeBuilder
    .withServiceExecutor(DefaultExecutorService)
    .bindHttp(port, "0.0.0.0")
    .mountService(LogEndpoint.service(), "/")
    .start

}
