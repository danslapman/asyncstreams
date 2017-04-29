package asyncstreams

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scalaz.MonadError
import scalaz.std.scalaFuture._

abstract class ZeroError[T, F[+_]: λ[`x[+_]` => MonadError[x, Throwable]]] {
  val zeroElement: T
}

class FutureZeroError(implicit ex: ExecutionContext) extends ZeroError[Throwable, Future] {
  override val zeroElement: Throwable = new NoSuchElementException
}
