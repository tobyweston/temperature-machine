package bad.robot.temperature

import scala.BigDecimal._

object PercentageDifference {
  
  def percentageDifference(oldValue: Double, newValue: Double): Double = {
    def percentageIncrease = (newValue - oldValue) / oldValue * 100
    def percentageDecrease = (oldValue - newValue) / oldValue * 100
    
    val result = if (newValue > oldValue) percentageIncrease else -percentageDecrease
    
    round(result)
  }

  private def round(value: Double) = BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble

}
