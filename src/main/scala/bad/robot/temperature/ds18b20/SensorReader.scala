package bad.robot.temperature.ds18b20

import java.io.File

import bad.robot.temperature._

import scala.io.Source
import scalaz.\/._
import scalaz.syntax.std.option._


object SensorReader {
  def apply(file: File) = new SensorReader(file)
}

class SensorReader(file: File) extends TemperatureReader {

  def read = {
    for {
      file        <- fromTryCatchNonFatal(Source.fromFile(file)).leftMap(FileError)
      data        <- file.getLines().toList.headOption.toRightDisjunction(UnexpectedError("Problem reading file, is it empty?"))
      temperature <- Parser.parse(data)
    } yield temperature
  }
}
