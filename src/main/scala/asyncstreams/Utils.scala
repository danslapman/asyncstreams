package asyncstreams

import asyncstreams.typeclass.ZeroK
import cats.{Alternative, Monad, MonadError}

import scala.language.higherKinds

object Utils {
  implicit class IterableToAS[T](it: Iterable[T]) {
    def toAS[F[+_]: Monad](implicit methods: ASImpl[F]): AsyncStream[F, T] = methods.fromIterable(it)
  }

  def monadErrorFilter[F[+_]: λ[`x[+_]` => MonadError[x, Throwable]] : ZeroK]: Alternative[F] =
    new AlternativeForMonadError[F]
}
