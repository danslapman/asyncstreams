package asyncstreams.monixTask

import asyncstreams.typeclass.ZeroK
import monix.eval.Task

object TaskZeroK {
  implicit val taskZeroK = new ZeroK[Task] {
    override def zero[A] = Task.raiseError(new NoSuchElementException)
  }
}
