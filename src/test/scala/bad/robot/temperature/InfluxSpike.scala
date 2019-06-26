package bad.robot.temperature

import bad.robot.temperature.rrd.{Host, Seconds}
import com.paulgoldbaum.influxdbclient.{InfluxDB, QueryResult, Record}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object InfluxSpike extends App {

  import scala.concurrent.ExecutionContext.Implicits.global
  
  val influx = InfluxDB.connect("telephone.local", 8086)

  val database = influx.selectDatabase("temperatures")
  
  val future = database.query("SELECT * from temperature")
  private val result: QueryResult = Await.result(future, Duration("10 seconds"))
  println(result.series.head.records.map(toThing).mkString("\n"))
  
  influx.close()
  
  def toThing(record: Record): String = {
    val host = record("host").asInstanceOf[String]
    val timezone = record("timezone").asInstanceOf[String]
    val temperature1: Double = record("sensor-1").asInstanceOf[BigDecimal].toDouble
    val temperature2 = record("sensor-2").asInstanceOf[BigDecimal].toDouble
    val temperature3 = record("sensor-3").asInstanceOf[BigDecimal].toDouble
    val temperature4 = record("sensor-4").asInstanceOf[BigDecimal].toDouble
    
    val m = Measurement(
      Host(host, None, Some(timezone)),
      Seconds(0),
      List(
        SensorReading("sensor-1", Temperature(temperature1)),
        SensorReading("sensor-2", Temperature(temperature2)),
        SensorReading("sensor-3", Temperature(temperature3)),
        SensorReading("sensor-4", Temperature(temperature4))
      )
    )
    m.toString
  }
}
