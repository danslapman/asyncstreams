import scala.language.higherKinds
import scalaz.Monad

package object asyncstreams {
  final val END: Null = null
  final def ENDF[F[+_]: Monad](implicit fm: Monad[F]): F[Null] = fm.point(END)

  implicit def asyncStreamInstance[F[+_]: Monad]: Monad[AsyncStream[F, ?]] = new AsyncStreamMonad[F]

  def fStateInstance[F[+_]: Monad, S] = new FStateMonad[F, S]

  object monadops extends FStateMonadFunctions with AsyncStreamMonadFunctions
}
