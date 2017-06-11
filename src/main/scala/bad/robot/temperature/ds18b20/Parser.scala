package bad.robot.temperature.ds18b20

import bad.robot.temperature._

import scalaz.{-\/, \/, \/-}

object Parser {

  val Space = " "
  val Hex = "([0-9a-fA-F]{2})" + Space
  val TempLsb = Hex
  val TempMsb = Hex
  val CrcByte = Hex
  val Crc = ": (crc=[0-9a-fA-F]{2})" + Space
  val CrcResult = "(YES|NO)"
  val Rest = """([\s\w=-]*)"""

  val SensorOutput = (TempLsb + TempMsb + Hex + Hex + Hex + Hex + Hex + Hex + CrcByte + Crc + CrcResult + Rest).r

  def parse(content: String): Error \/ Temperature = {
    content match {
      case SensorOutput(lsb, msb, _, _, _, _, _, _, _, _, "YES", _) => \/-(Temperature(Integer.parseInt(msb + lsb, 16).shortValue / 16.0))
      case SensorOutput(_, _, _, _, _, _, _, _, _, _, "NO", _)      => -\/(CrcFailure())
      case data @ _                                                 => -\/(UnexpectedError(s"Failed to recognise sensor data \n$data"))
    }
  }
}