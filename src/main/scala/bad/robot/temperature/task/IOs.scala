package bad.robot.temperature.task

import java.util.concurrent.Executors._

import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import bad.robot.temperature.rrd.RrdFile.MaxSensors
import bad.robot.temperature.rrd.{Host, RrdFile}
import bad.robot.temperature.task.Scheduler.ScheduledExecutorServiceOps
import bad.robot.temperature.{JsonExport, TemperatureWriter, XmlExport}
import bad.robot.logging._
import scala.concurrent.duration._
import scala.language.postfixOps
import cats.effect.IO

object IOs {

  def init(hosts: List[Host]) = {
    Log.info(s"RRD initialising for ${hosts.map(_.name).mkString("'", "', '", "'")} (with up to $MaxSensors sensors each)...")
    IO.pure(RrdFile.exists).map {
      case false => RrdFile(hosts, 30 seconds).create()
      case _     => Log.info("Ok")
    }
  }

  def record(source: Host, sensors: List[SensorFile], destination: TemperatureWriter) = {
    val executor = newSingleThreadScheduledExecutor(TemperatureMachineThreadFactory("reading-thread"))
    for {
      _     <- IO.pure(Log.info(s"Monitoring sensor file(s) on '${source.name}' ${sensors.mkString("\n\t", "\n\t", "\n")}"))
      tasks <- IO.pure(executor.schedule(30 seconds, RecordTemperature(source, SensorReader(sensors), destination, Log)))
    } yield tasks
  }

  def graphing(implicit hosts: List[Host]) = {
    val executor = newScheduledThreadPool(3, TemperatureMachineThreadFactory("graphing-thread"))
    for {
      _ <- IO.pure(executor.schedule(90 seconds, GenerateGraph(24 hours)))
      _ <- IO.pure(executor.schedule(12 hours, GenerateGraph(7 days)))
      _ <- IO.pure(executor.schedule(24 hours, GenerateGraph(30 days)))
    } yield ()
  }

  def exportXml(implicit hosts: List[Host]) = {
    val executor = newSingleThreadScheduledExecutor(TemperatureMachineThreadFactory("xml-export-thread"))
    IO.pure(executor.schedule(100 seconds, XmlExport(24 hours)))
  }

  def exportJson(implicit hosts: List[Host]) = {
    val executor = newSingleThreadScheduledExecutor(TemperatureMachineThreadFactory("json-export-thread"))
    IO.pure(executor.schedule(100 seconds, JsonExport(24 hours)))
  }

}
