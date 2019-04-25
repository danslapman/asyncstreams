package asyncstreams

import cats.{Monad, MonoidK}
import cats.syntax.functor._

import scala.annotation.unchecked.{uncheckedVariance => uV}
import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds

trait TestHelpers {
  implicit class AsyncStreamTestOps[F[_]: Monad: MonoidK, A](stream: AsyncStream[F, A]) {
    def to[Col[+_]](implicit cbf: CanBuildFrom[Nothing, A, Col[A @uV]]): F[Col[A]] =
      stream.foldLeft(cbf())((col, el) => col += el).map(_.result())
  }
}
