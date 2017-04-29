package asyncstreams

import scala.language.higherKinds
import scalaz.{Monad, MonadError, MonadPlus}

object Utils {
  implicit class IterableToAS[T](it: Iterable[T]) {
    def toAS[F[+_]: Monad](implicit methods: ASImpl[F]): AsyncStream[F, T] = methods.fromIterable(it)
  }

  //TODO: set proper variance
  def monadErrorFilter[F[+_]: MonadError[?[_], Throwable] : ZeroError[Throwable, ?[_]]]: MonadPlus[F] =
    new MonadFilterForMonadError[F]
}
