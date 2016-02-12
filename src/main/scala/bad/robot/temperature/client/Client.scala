package bad.robot.temperature.client

import bad.robot.temperature.FailedToFindFile
import bad.robot.temperature.ds18b20.SensorFile
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.rrd.Host
import bad.robot.temperature.rrd.RrdFile._
import bad.robot.temperature.task.Tasks

import scalaz.concurrent.Task

object Client extends App {

  val location = sys.props.getOrElse("sensor.location", BaseFolder)

  SensorFile.find(location) match {
    case Nil     => println(FailedToFindFile(location).message)
    case sensors => start(sensors)
  }

  private def start(sensors: List[SensorFile]) = {
    val client = for {
      _      <- Task.delay(println(s"Initialising client ${Host.name.name} (with ${sensors.size} of a maximum of $MaxSensors sensors)..."))
      server <- Task.delay(DiscoveryClient.discover)
      tasks  <- Tasks.record(sensors, HttpUpload(server))
    } yield ()

    client.run
  }

}
