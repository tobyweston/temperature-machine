package bad.robot.temperature

object AutoClosing {
  def closingAfterUse[A <: AutoCloseable, B](resource: A)(f: A => B): B = try {
    f(resource)
  } finally {
    resource.close()
  }
}
