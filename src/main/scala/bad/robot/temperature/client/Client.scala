package bad.robot.temperature.client

import bad.robot.temperature.ds18b20.SensorFile
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.rrd.Host
import bad.robot.temperature.rrd.RrdFile._
import bad.robot.temperature.task.Tasks

import scalaz.concurrent.Task

object Client extends App {

  findSensorsAndExecute(start)

  def start(sensors: List[SensorFile]) = {
    for {
      _      <- Task.delay(println(s"Initialising client '${Host.local.name}' (with ${sensors.size} of a maximum of $MaxSensors sensors)..."))
      server <- Task.delay(DiscoveryClient.discover)
      tasks  <- Tasks.record(Host.local.trim, sensors, HttpUpload(server))
    } yield ()
  }

}
