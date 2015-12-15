package bad.robot.temperature

import java.util.Date

package object rrd {

  def now() = Seconds(timeInSeconds(new Date()))

  def timeInSeconds(date: Date) = (date.getTime + 499L) / 1000L
}
