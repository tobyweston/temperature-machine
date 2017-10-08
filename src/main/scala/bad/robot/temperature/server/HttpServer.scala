package bad.robot.temperature.server

import java.lang.Math._
import java.time.Clock
import java.util.concurrent.{CountDownLatch, ExecutorService}
import java.util.concurrent.Executors._

import bad.robot.temperature.ErrorOnTemperatureSpike
import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import bad.robot.temperature.rrd.{Host, Rrd}
import bad.robot.temperature.task.TemperatureMachineThreadFactory
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.middleware.CORS
import org.http4s.server.syntax.ServiceOps
import org.http4s.server.{Server => Http4sServer}

import scalaz.concurrent.Task

object HttpServer {
  def apply(port: Int, monitored: List[Host]): Task[HttpServer] = Task.delay {
    val server = new HttpServer(port, monitored)
    server.build().unsafePerformSync
    server
  }
}

class HttpServer(port: Int, monitored: List[Host]) {

  private val latch = new CountDownLatch(1)

  def awaitShutdown(): Task[Unit] = Task.delay(latch.await())

  def shutdown(): Task[Unit] = Task.delay(latch.countDown())

  private val DefaultExecutorService: ExecutorService = {
    newFixedThreadPool(max(4, Runtime.getRuntime.availableProcessors), TemperatureMachineThreadFactory("http-server"))
  }

  private def build(): Task[Http4sServer] = BlazeBuilder
    .withServiceExecutor(DefaultExecutorService)
    .bindHttp(port, "0.0.0.0")
    .mountService(services(), "/")
    .start

  private def services() = {
    CORS(
      TemperatureEndpoint.service(SensorReader(SensorFile.find()), ErrorOnTemperatureSpike(Rrd(monitored)))(Clock.systemDefaultZone) ||
      ConnectionsEndpoint.service(Clock.systemDefaultZone) ||
      StaticFiles.service ||
      StaticResources.service
    )
  }
}