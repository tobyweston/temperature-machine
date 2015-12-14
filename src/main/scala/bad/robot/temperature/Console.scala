package bad.robot.temperature

import java.text.SimpleDateFormat
import java.util.Calendar

case class Console() extends TemperatureWriter {

  def write(temperature: Temperature): Unit = println(s"$currentTime ${temperature.celsius} °C")

  def currentTime = {
    val today = Calendar.getInstance().getTime
    val format = new SimpleDateFormat("dd/MM/YYYY HH:mm:ss")
    format.format(today)
  }

}
