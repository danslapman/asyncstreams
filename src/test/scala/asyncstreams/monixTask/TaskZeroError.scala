package asyncstreams.monixTask

import asyncstreams.ZeroError
import monix.eval.Task
import monix.scalaz._

object TaskZeroError {
  implicit val ze = new ZeroError[Throwable, Task] {
    override val zeroElement: Throwable = new NoSuchElementException
  }
}
