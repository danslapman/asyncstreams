package asyncstreams

class Pair[A, B](fp: A, sp: => B) {
  val first = fp
  lazy val second = sp
}

object Pair {
  def apply[A, B](first:  A, second: => B) = new Pair[A, B](first, second)
}
