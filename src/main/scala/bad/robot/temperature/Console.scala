package bad.robot.temperature

import java.text.SimpleDateFormat
import java.util.Calendar

case class Console() extends TemperatureWriter {

  def write(id: SensorId, temperature: Temperature): Unit = println(s"${id.ordinal} $currentTime ${temperature.celsius} °C")

  def currentTime = {
    val today = Calendar.getInstance().getTime
    val date = new SimpleDateFormat("dd/MM/YYYY HH:mm:ss")
    date.format(today)
  }

}
