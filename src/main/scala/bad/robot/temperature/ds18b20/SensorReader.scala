package bad.robot.temperature.ds18b20

import bad.robot.temperature.AutoClosing._
import bad.robot.temperature._
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.ds18b20.SensorReader._
import bad.robot.temperature.rrd.Host
import bad.robot.temperature.rrd.Seconds.{apply => _, _}

import scala.io.Source
import scalaz.Scalaz._
import scalaz.\/
import scalaz.\/._


object SensorReader {

  def apply(host: Host, files: List[SensorFile]) = new SensorReader(host, files)

  private val toReading: SensorFile => Error \/ SensorReading = file => {
    for {
      source      <- fromTryCatchNonFatal(Source.fromFile(file)).leftMap(FileError)
      data        <- closingAfterUse(source)(_.getLines().toList).headOption.toRightDisjunction(UnexpectedError("Problem reading file, is it empty?"))
      temperature <- Parser.parse(data)
    } yield SensorReading(file.getParentFile.getName, temperature)
  }

}

class SensorReader(host: Host, sensors: List[SensorFile]) extends TemperatureReader {

  def read: Error \/ Measurement = {
    for {
      files         <- sensors.toNel.toRightDisjunction(FailedToFindFile(BaseFolder))
      temperatures  <- files.map(toReading).sequenceU
    } yield Measurement(host, now(), temperatures.toList)
  }

}
