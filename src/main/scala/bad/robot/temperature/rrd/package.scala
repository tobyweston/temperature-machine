package bad.robot.temperature

import java.io.File
import java.util.Date

import org.rrd4j.core.RrdDb

import scala.concurrent.duration.Duration

package object rrd {

  val anHour = Duration(1, "hour")
  val aDay = Duration(24, "hours")
  val aWeek = Duration(7, "days")
  val aMonth = Duration(30, "days")

  def now() = Seconds(timeInSeconds(new Date()))

  def timeInSeconds(date: Date) = (date.getTime + 499L) / 1000L

  implicit def fileToString(file: File): String = file.getAbsolutePath

  implicit class FileOps(file: File) {
    def /(child: String): File = new File(file, child)
  }

  implicit class RrdDbOps(database: RrdDb) {
    def hasValuesFor(datasource: String): Boolean = database.getDatasource(database.getDsIndex(datasource)).getAccumValue != 0.0
  }
}
