package asyncstreams

import asyncstreams.typeclass.ZeroK
import cats.{Alternative, Monad, MonadError}
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._

import scala.language.higherKinds

class ASInstanceForMonadError[F[+_]](implicit fme: MonadError[F, Throwable], zk: ZeroK[F]) extends Monad[AsyncStream[F, ?]] with Alternative[AsyncStream[F, ?]] {
  override def empty[A] = AsyncStream(zk.zero)

  override def combineK[A](x: AsyncStream[F, A], y: AsyncStream[F, A]): AsyncStream[F, A] = AsyncStream {
    x.data.map(step => Step(step.value, combineK(step.rest, y))).handleErrorWith(_ => y.data)
  }

  override def pure[A](x: A) = AsyncStream(Step(x, empty[A]).pure[F])

  override def flatMap[A, B](fa: AsyncStream[F, A])(f: A => AsyncStream[F, B]): AsyncStream[F, B] = AsyncStream {
    fa.data.flatMap(step => f(step.value).data.map(step2 => Step(step2.value, combineK(step2.rest, flatMap(step.rest)(f)))))
  }

  override def tailRecM[A, B](a: A)(f: A => AsyncStream[F, Either[A, B]]) = ???
}
