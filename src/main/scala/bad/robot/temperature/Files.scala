package bad.robot.temperature

import java.io.File

object Files {

  val path = new File(sys.props("user.home")) / ".temperature"

  path.mkdirs()

  implicit def fileToString(file: File): String = file.getAbsolutePath

}
