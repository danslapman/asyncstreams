package asyncstreams

class Step[A, B](fp: A, sp: => B) {
  val value = fp
  lazy val rest = sp
}

object Step {
  def apply[A, B](value:  A, rest: => B) = new Step[A, B](value, rest)
}
