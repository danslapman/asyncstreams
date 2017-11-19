import cats.Monad
import cats.syntax.applicative._

import scala.language.higherKinds

package object asyncstreams {
  implicit class AsyncStreamOps[F[+_]: Monad, A](stream: => AsyncStream[F, A]) {
    def ~::(el: A) = AsyncStream(Step(el, stream).pure[F])
  }

  implicit class IterableToAS[T](it: Iterable[T]) {
    def toAS[F[+_]: Monad](implicit methods: ASImpl[F]): AsyncStream[F, T] = methods.fromIterable(it)
  }
}
