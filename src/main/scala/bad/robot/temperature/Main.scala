package bad.robot.temperature

import java.net.InetAddress

import bad.robot.temperature.TemperatureMachineThreadFactory._
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import bad.robot.temperature.rrd.{Rrd, RrdFile}
import bad.robot.temperature.server.Server

import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends App {

  val Port = 11900

  val location = sys.props.getOrElse("sensor.location", BaseFolder)

  SensorFile.find(location) match {
    case Nil   => println(FailedToFindFile(location).message)
    case files => start(files)
  }

  private def start(sensors: List[SensorFile]) = {
    implicit val numberOfSensors = sensors.size

    print(s"RRD initialising (with $numberOfSensors of a maximum of 5 sensors)...")
    if (!RrdFile.exists) RrdFile(30 seconds).create() else println("Ok")

    print(s"Monitoring sensor file(s) ${sensors.mkString("\n\t", "\n\t", "\n")}")
    Scheduler(30 seconds, createThreadPool("reading-thread")).start(Measurement(SensorReader(sensors), Rrd()))

    Scheduler(90 seconds, createThreadPool("graphing-thread")).start(GenerateGraph(24 hours))
    Scheduler(12 hours, createThreadPool("graphing-thread")).start(GenerateGraph(7 days))
    Scheduler(24 hours, createThreadPool("graphing-thread")).start(GenerateGraph(30 days))

    Scheduler(100 seconds, createThreadPool("xml-export-thread")).start(XmlExport(24 hours))
    println("Starting monitoring threads...Ok")

    println(s"Server started on http://${InetAddress.getLocalHost.getHostAddress}:$Port")
    Server(Port).start()
  }
}
