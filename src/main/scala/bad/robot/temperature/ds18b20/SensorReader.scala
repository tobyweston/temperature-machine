package bad.robot.temperature.ds18b20

import bad.robot.temperature._

import scala.io.Source
import scalaz.\/._
import scalaz.syntax.std.option._


object SensorReader {
  def apply(filename: String) = new SensorReader(filename)
}

class SensorReader(filename: String) extends TemperatureReader {

  def read = {
    for {
      file        <- fromTryCatchNonFatal(Source.fromFile(filename)).leftMap(FileError)
      data        <- file.getLines().toList.headOption.toRightDisjunction(UnexpectedError("Problem reading file, is it empty?"))
      temperature <- Parser.parse(data)
    } yield temperature
  }
}
