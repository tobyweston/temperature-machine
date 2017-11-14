package bad.robot.temperature.ds18b20

import bad.robot.temperature.AutoClosing._
import bad.robot.temperature._
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.ds18b20.SensorReader._

import scala.io.Source
import scalaz.Scalaz._
import scalaz.\/
import scalaz.\/._


object SensorReader {

  def apply(files: List[SensorFile]) = new SensorReader(files)

  private val toReading: SensorFile => Error \/ SensorReading = file => {
    for {
      source      <- fromTryCatchNonFatal(Source.fromFile(file)).leftMap(FileError)
      data        <- closingAfterUse(source)(_.getLines().toList).headOption.toRightDisjunction(UnexpectedError("Problem reading file, is it empty?"))
      temperature <- Parser.parse(data)
    } yield SensorReading(file.getParentFile.getName, temperature)
  }

}

class SensorReader(sensors: List[SensorFile]) extends TemperatureReader {

  def read: Error \/ List[SensorReading] = {
    for {
      files         <- sensors.toNel.toRightDisjunction(FailedToFindFile(BaseFolder))
      temperatures  <- files.map(toReading).sequenceU
    } yield temperatures.toList
  }

}
