package asyncstreams

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scalaz.MonadError
import scalaz.std.scalaFuture._

//TODO: proper variance
abstract class ZeroError[T, F[_]: MonadError[?[_], T]] {
  val zeroElement: T
}

class FutureZeroError(implicit ex: ExecutionContext) extends ZeroError[Throwable, Future] {
  override val zeroElement: Throwable = new NoSuchElementException
}
