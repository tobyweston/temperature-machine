package bad.robot.temperature

import bad.robot.temperature.ds18b20.Parser

import scala.io.Source
import scalaz.\/._
import scalaz.syntax.std.option._


object TemperatureReader {
  def apply(filename: String) = new TemperatureReader(filename)
}

class TemperatureReader(filename: String) {

  def read = {
    for {
      file        <- fromTryCatchNonFatal(Source.fromFile(filename)).leftMap(FileError)
      data        <- file.getLines().toList.headOption.toRightDisjunction(UnexpectedError("Problem reading file, is it empty?"))
      temperature <- Parser.parse(data)
    } yield temperature
  }
}
