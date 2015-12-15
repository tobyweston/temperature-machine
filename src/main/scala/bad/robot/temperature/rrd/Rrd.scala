package bad.robot.temperature.rrd

import java.util.Date

import bad.robot.temperature.{Temperature, TemperatureWriter}

case class Rrd() extends TemperatureWriter {
  def write(temperature: Temperature) = RrdTemperature(new Date().getTime, temperature).apply()
}
