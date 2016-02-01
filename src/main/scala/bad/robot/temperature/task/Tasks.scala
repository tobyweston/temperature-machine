package bad.robot.temperature.task

import java.util.concurrent.Executors._

import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import bad.robot.temperature.rrd.{Rrd, RrdFile}
import bad.robot.temperature.server.Server
import bad.robot.temperature.task.Scheduler._
import bad.robot.temperature.{TemperatureWriter, XmlExport}

import scala.concurrent.duration._
import scala.language.postfixOps
import scalaz.concurrent.Task

object Tasks {

  def init(implicit numberOfSensors: Int) = {
    print(s"RRD initialising (with $numberOfSensors of a maximum of 5 sensors)...")
    Task.delay(RrdFile.exists).map {
      case true => RrdFile(30 seconds).create()
      case _ => println("Ok")
    }
  }

  def record(sensors: List[SensorFile], to: TemperatureWriter) = {
    val executor = newScheduledThreadPool(1, TemperatureMachineThreadFactory("reading-thread"))
    for {
      _     <- Task.delay(print(s"Monitoring sensor file(s) ${sensors.mkString("\n\t", "\n\t", "\n")}"))
      tasks <- Task.delay(executor.schedule(30 seconds, RecordTemperature(SensorReader(sensors), to)))
    } yield tasks
  }

  def graphing(implicit numberOfSensors: Int) = {
    val executor = newScheduledThreadPool(3, TemperatureMachineThreadFactory("graphing-thread"))
    for {
      _ <- Task.delay(executor.schedule(90 seconds, GenerateGraph(24 hours)))
      _ <- Task.delay(executor.schedule(12 hours, GenerateGraph(7 days)))
      _ <- Task.delay(executor.schedule(24 hours, GenerateGraph(30 days)))
    } yield ()
  }

  def exportXml(implicit numberOfSensors: Int) = {
    val executor = newScheduledThreadPool(1, TemperatureMachineThreadFactory("xml-export-thread"))
    Task.delay(executor.schedule(100 seconds, XmlExport(24 hours)))
  }

  def application(sensors: List[SensorFile]) = {
    implicit val numberOfSensors = sensors.size

    for {
      _ <- Tasks.init
      _ <- Tasks.record(sensors, Rrd())
      _ <- Tasks.graphing
      _ <- Tasks.exportXml
      _ <- Server.http
    } yield ()
  }

}
