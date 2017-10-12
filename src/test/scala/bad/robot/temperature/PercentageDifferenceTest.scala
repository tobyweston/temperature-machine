package bad.robot.temperature

import bad.robot.temperature.PercentageDifference.percentageDifference
import org.specs2.mutable.Specification

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
  
}
