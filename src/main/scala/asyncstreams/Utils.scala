package asyncstreams

import cats.Monad

import scala.language.higherKinds

object Utils {
  implicit class IterableToAS[T](it: Iterable[T]) {
    def toAS[F[+_]: Monad](implicit methods: ASImpl[F]): AsyncStream[F, T] = methods.fromIterable(it)
  }
}
