package bad.robot.temperature.rrd

object RpnGenerator {

  sealed abstract case class Aggregator(value: String) {
    def toRpn(numberOfDataSources: Int) = Stream.iterate(value, numberOfDataSources)(_ => value).mkString(",")
  }
  object Max extends Aggregator("MAX")
  object Min extends Aggregator("MIN")

  def generateRpn(sensors: List[String], aggregator: Aggregator) = {
    sensors match {
      case sensor :: Nil => sensor + "," + aggregator.toRpn(1)
      case items => items.mkString("", ",", ",") + aggregator.toRpn(sensors.size - 1)
    }
  }
}
