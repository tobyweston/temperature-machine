package bad.robot.temperature.task

import java.util.concurrent.Executors._

import bad.robot.logging._
import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import bad.robot.temperature.rrd.RrdFile.MaxSensors
import bad.robot.temperature.rrd.{Host, RrdFile}
import bad.robot.temperature.server.AllTemperatures
import bad.robot.temperature.task.Scheduler.ScheduledExecutorServiceOps
import bad.robot.temperature.{FixedTimeMeasurementWriter, JsonExport, TemperatureWriter, XmlExport}
import cats.effect.IO
import cats.implicits._

import scala.concurrent.duration._
import scala.language.postfixOps

object IOs {

  def init(hosts: List[Host]) = {
    for {
      exists <- RrdFile.exists
      _      <- info(s"Creating RRD for ${hosts.map(_.name).mkString("'", "', '", "'")} (with up to $MaxSensors sensors each)...").unlessA(exists)
      _      <- IO(RrdFile(hosts, 30 seconds).create()).unlessA(exists)
      _      <- info("RRD created Ok").unlessA(exists)
    } yield ()
  }

  def gather(temperatures: AllTemperatures, destination: FixedTimeMeasurementWriter) = {
    val frequency = 30 seconds
    val executor = newSingleThreadScheduledExecutor(TemperatureMachineThreadFactory("rrd-writing-thread"))
    for {
      _     <- info(s"Writing to the RRD every $frequency (sample times may be off by +/- $frequency, maybe a little more)")
      tasks <- IO(executor.schedule(frequency, RecordTemperatures(temperatures, destination, Log)))
    } yield tasks
  }
  
  def record(host: Host, sensors: List[SensorFile], destination: TemperatureWriter) = {
    val executor = newSingleThreadScheduledExecutor(TemperatureMachineThreadFactory("reading-thread"))
    for {
      _     <- info(s"Monitoring sensor file(s) on '${host.name}' ${sensors.mkString("\n\t", "\n\t", "\n")}")
      tasks <- IO(executor.schedule(1 second, RecordTemperature(SensorReader(host, sensors), destination, Log)))
    } yield tasks
  }

  def graphing(implicit hosts: List[Host]) = {
    val executor = newScheduledThreadPool(3, TemperatureMachineThreadFactory("graphing-thread"))
    for {
      _ <- IO(executor.schedule(90 seconds, GenerateGraph(24 hours)))
      _ <- IO(executor.schedule(12 hours, GenerateGraph(7 days)))
      _ <- IO(executor.schedule(24 hours, GenerateGraph(30 days)))
    } yield ()
  }

  def exportXml(implicit hosts: List[Host]) = {
    val executor = newSingleThreadScheduledExecutor(TemperatureMachineThreadFactory("xml-export-thread"))
    IO(executor.schedule(100 seconds, XmlExport(24 hours)))
  }

  def exportJson(implicit hosts: List[Host]) = {
    val executor = newSingleThreadScheduledExecutor(TemperatureMachineThreadFactory("json-export-thread"))
    IO(executor.schedule(100 seconds, JsonExport(24 hours)))
  }

}
