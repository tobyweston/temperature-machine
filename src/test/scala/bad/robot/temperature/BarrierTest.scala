package bad.robot.temperature

import org.specs2.mutable.Specification

import scala.Double._

class BarrierTest extends Specification {

  "No breach" >> {
    val barrier = Barrier(5)
    barrier.breached(Temperature(10.23), Temperature(11.22)) must_== false
    barrier.breached(Temperature(-10.23), Temperature(-10.23)) must_== false
    barrier.breached(Temperature(0), Temperature(0)) must_== false
    barrier.breached(Temperature(23.12), Temperature(23.12)) must_== false
    barrier.breached(Temperature(23.12), Temperature(24.11)) must_== false
  }
  
  "Positive breach" >> {
    val barrier = Barrier(1)
    barrier.breached(Temperature(10.23), Temperature(11.24)) must_== true
    barrier.breached(Temperature(10.24), Temperature(11.23)) must_== false
  }
  
  "Negative breach" >> {
    val barrier = Barrier(1)
    barrier.breached(Temperature(-10.23), Temperature(-11.24)) must_== true
    barrier.breached(Temperature(-10.24), Temperature(-11.23)) must_== false
  }

  "NaN" >> {
    val barrier = Barrier(1)
    barrier.breached(Temperature(-10.23), Temperature(NaN)) must_== false
    barrier.breached(Temperature(NaN), Temperature(10.23)) must_== false
    barrier.breached(Temperature(NaN), Temperature(NaN)) must_== false
  }
  
}
