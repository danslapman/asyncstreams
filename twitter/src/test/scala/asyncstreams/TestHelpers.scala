package asyncstreams

import cats.{Monad, MonoidK}
import cats.syntax.functor._

import scala.collection.compat._
import scala.language.higherKinds

trait TestHelpers {
  implicit class AsyncStreamTestOps[F[_]: Monad: MonoidK, A](stream: AsyncStream[F, A]) {
    def to[Col[+_]](col: Factory[A, Col[A]]): F[Col[A]] =
      stream.foldLeft(col.newBuilder)((col, el) => col += el).map(_.result())
  }
}
