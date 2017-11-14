package bad.robot.temperature

import java.lang.Math._

import bad.robot.temperature.PercentageDifference.percentageDifference
import org.specs2.mutable.Specification

import scala.Double._

class PercentageDifferenceTest extends Specification {

  "Increase as a percentage (and rounding)" >> {
    percentageDifference(oldValue = 23.0, newValue = 23.0) must_== 0
    percentageDifference(oldValue = 23.0, newValue = 24.0) must_== 4.35
    percentageDifference(oldValue = 23.0, newValue = 30.0) must_== 30.43
    percentageDifference(oldValue = 14.42934783, newValue = 34.76785714) must_== 140.95
  }
  
  "Decrease as a percentage (and rounding)" >> {
    percentageDifference(oldValue = 23.0, newValue = 23.0) must_== 0
    percentageDifference(oldValue = 24.0, newValue = 23.0) must_== -4.17
    percentageDifference(oldValue = 39.0, newValue = 23.0) must_== -41.03
    percentageDifference(oldValue = 34.76785714, newValue = 14.42934783) must_== -58.5
  }
  
  "What's the percentage increase of zero" >> {
    percentageDifference(oldValue = 0, newValue = 100).isNaN must_== true
    percentageDifference(oldValue = 0, newValue = 32.625).isNaN must_== true
  }
  
  "Not a number / infinity doesn't blow up" >> {
    percentageDifference(oldValue = 22.0, newValue = NaN).isNaN must_== true
    percentageDifference(oldValue = NaN, newValue = 22.0).isNaN must_== true

    "Sense check that nothing weird happens if we try to make comparisons against NaN" >> {
      percentageDifference(oldValue = 22.0, newValue = NaN) >= 30 must_== false
      percentageDifference(oldValue = NaN, newValue = 22.0) >= 30 must_== false
      percentageDifference(oldValue = NaN, newValue = NaN)  >= 30 must_== false
      abs(percentageDifference(oldValue = NaN, newValue = NaN)) >= 30 must_== false
    }

    percentageDifference(oldValue = 22.0, newValue = PositiveInfinity).isNaN must_== true
    percentageDifference(oldValue = PositiveInfinity, newValue = 22.0).isNaN must_== true

    percentageDifference(oldValue = 22.0, newValue = NegativeInfinity).isNaN must_== true
    percentageDifference(oldValue = NegativeInfinity, newValue = 22.0).isNaN must_== true
  }

}
