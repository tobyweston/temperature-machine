package bad.robot.temperature.client

import bad.robot.temperature.FailedToFindFile
import bad.robot.temperature.ds18b20.SensorFile
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.task.Tasks

import scalaz.concurrent.Task

object Client extends App {

  val location = sys.props.getOrElse("sensor.location", BaseFolder)

  SensorFile.find(location) match {
    case Nil => println(FailedToFindFile(location).message)
    case files => start(files)
  }

  private def start(sensors: List[SensorFile]) = {
    val client = for {
      server <- Task.delay(DiscoveryClient.discover)
      tasks  <- Tasks.record(sensors, HttpUpload(server))
    } yield ()

    client.run
  }

}
