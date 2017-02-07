package asyncstreams

class Step[A, B](fp: A, sp: => B) {
  val value: A = fp
  lazy val rest: B = sp
}

object Step {
  def apply[A, B](value:  A, rest: => B) = new Step[A, B](value, rest)
}
