import scala.concurrent.{ExecutionContext, Future}
import scalaz.Monad

package object asyncstreams {
  final val END: Null = null
  final def ENDF(implicit executor: ExecutionContext): Future[Null] = Future(END)

  implicit def asyncStreamInstance(implicit executor: ExecutionContext): Monad[AsyncStream] =
    new AsyncStreamMonad

  def fStateInstance[S](implicit executor: ExecutionContext) = new FStateMonad[S]

  object monadops extends FStateMonadFunctions with AsyncStreamMonadFunctions
}
