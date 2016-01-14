package bad.robot.temperature

import java.text.SimpleDateFormat
import java.util.Calendar

case class Console() extends TemperatureWriter {

  def write(temperatures: List[Temperature]): Unit = println(s"$currentTime ${temperatures.map(_.celsius).mkString(", ")} °C")

  def currentTime = {
    val today = Calendar.getInstance().getTime
    val date = new SimpleDateFormat("dd/MM/YYYY HH:mm:ss")
    date.format(today)
  }

}
