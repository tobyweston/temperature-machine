package bad.robot.temperature.ds18b20

import java.io.File
import java.nio.file.StandardWatchEventKinds._
import java.nio.file.{FileSystems, Path, WatchEvent, WatchKey}

import scala.collection.JavaConverters._
import scalaz.concurrent.Task

class Watcher {

  private val watcher = FileSystems.getDefault.newWatchService

  def watch(sensors: List[SensorFile]) = {
    for {
      _ <- Task.delay(sensors.map(register))
      _ <- Task.delay(listen)
    } yield ()
  }

  val register: SensorFile => WatchKey = file => {
    println(s"listening for changes to ${file.getParent}")
    file.toPath.getParent.register(watcher, ENTRY_MODIFY, ENTRY_DELETE, ENTRY_CREATE, OVERFLOW)
  }
  
  def listen() = {
    println(s"listening...")
    while (true) {
      val key = watcher.take
      val events: Seq[WatchEvent[_]] = key.pollEvents().asScala
      events.foreach(event => {
        event.kind match {
          case ENTRY_MODIFY =>
            val file: Path = event.context().asInstanceOf[Path]
            val path: Path = key.watchable().asInstanceOf[Path]
            println(s"event = ${file} ${key.watchable()}")
            if (file.endsWith("w1_slave")) {
              val reading = SensorReader(List(new File(path.toFile, "w1_slave"))).read
              println(s"reading = ${reading}")
            }
          case e @ _ => println(s"Event ${e.`type`()} ${e.name()}") 
        }
        val valid = key.reset()
        println(s"valid reset = ${valid}")
      })
    }  
  }
  
  SensorFile.findSensorsAndExecute(watch).leftMap(error => println(error.message))
  
}

object Watcher extends App {
  new Watcher()
}