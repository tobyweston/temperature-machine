package bad.robot.temperature.client

import java.util.concurrent.CountDownLatch

import bad.robot.temperature.ds18b20.SensorFile
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.rrd.Host
import bad.robot.temperature.rrd.RrdFile._
import bad.robot.temperature.task.Tasks

import scalaz.concurrent.Task

object Client extends App {

  private val latch = new CountDownLatch(1)

  private val client: List[SensorFile] => Task[Unit] = sensors => {
    for {
      _      <- Task.delay(println(s"Initialising client '${Host.local.name}' (with ${sensors.size} of a maximum of $MaxSensors sensors)..."))
      server <- Task.delay(DiscoveryClient.discover)
      _      <- Task.delay(println(s"Server discovered on ${server.getHostAddress}, monitoring temperatures..."))
      _      <- Tasks.record(Host.local.trim, sensors, HttpUpload(server))
      _      <- awaitShutdown()
    } yield ()
  }

  private def awaitShutdown(): Task[Unit] = Task.delay(latch.await())

  findSensorsAndExecute(client).leftMap(error => println(error.message))

}
