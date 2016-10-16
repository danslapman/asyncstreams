import scala.concurrent.{ExecutionContext, Future}
import scalaz.{Monad, StateT}

package object asyncstreams {
  type FState[S, A] = StateT[Future, S, A]
  type Chunk[A, B] = Option[Pair[A, B]]

  implicit def asyncStreamInstance(implicit executor: ExecutionContext): Monad[AsyncStream] =
    new AsyncStreamMonad
}
