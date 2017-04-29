package asyncstreams

import scala.language.higherKinds
import scalaz.{Monad, MonadError, MonadPlus}

object Utils {
  implicit class IterableToAS[T](it: Iterable[T]) {
    def toAS[F[+_]: Monad](implicit methods: ASImpl[F]): AsyncStream[F, T] = methods.fromIterable(it)
  }

  def monadErrorFilter[F[+_]: λ[`x[+_]` => MonadError[x, Throwable]] : λ[`x[+_]` => ZeroError[Throwable, x]]]: MonadPlus[F] =
    new MonadFilterForMonadError[F]
}
