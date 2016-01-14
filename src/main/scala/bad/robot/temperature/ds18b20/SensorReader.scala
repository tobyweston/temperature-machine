package bad.robot.temperature.ds18b20

import bad.robot.temperature._
import bad.robot.temperature.ds18b20.SensorReader._

import scala.io.Source
import scalaz.\/
import scalaz.\/._
import scalaz.std.list._
import scalaz.syntax.std.option._
import scalaz.syntax.traverse._


object SensorReader {

  def apply(files: List[SensorFile]) = new SensorReader(files)

  private val toReading: SensorFile => Error \/ Temperature = file => {
    for {
      file <- fromTryCatchNonFatal(Source.fromFile(file)).leftMap(FileError)
      data <- file.getLines().toList.headOption.toRightDisjunction(UnexpectedError("Problem reading file, is it empty?"))
      temperature <- Parser.parse(data)
    } yield temperature
  }

}

class SensorReader(sensors: List[SensorFile]) extends TemperatureReader {

  def read: Error \/ List[Temperature] = sensors.map(toReading).sequenceU

}
