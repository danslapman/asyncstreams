package asyncstreams

import asyncstreams.typeclass.ZeroK

import scala.language.higherKinds
import scalaz.{Monad, MonadError, MonadPlus}

object Utils {
  implicit class IterableToAS[T](it: Iterable[T]) {
    def toAS[F[+_]: Monad](implicit methods: ASImpl[F]): AsyncStream[F, T] = methods.fromIterable(it)
  }

  def monadErrorFilter[F[+_]: λ[`x[+_]` => MonadError[x, Throwable]] : ZeroK]: MonadPlus[F] =
    new MonadFilterForMonadError[F]
}
