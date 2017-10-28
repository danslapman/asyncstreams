package asyncstreams

import asyncstreams.typeclass.ZeroK
import cats.MonadError
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._

import scala.language.higherKinds

trait ASImpl[F[+_]] {
  def empty[A]: AsyncStream[F, A]
  def collectLeft[A, B](s: AsyncStream[F, A])(init: B)(f: (B, A) => B): F[B]
  def fromIterable[T](it: Iterable[T]): AsyncStream[F, T]
  def takeWhile[T](s: AsyncStream[F, T])(p: T => Boolean): AsyncStream[F, T]
  def isEmpty[T](s: AsyncStream[F, T]): F[Boolean]
}

class ASImplForMonadError[F[+_]](implicit fmp: MonadError[F, Throwable], ze: ZeroK[F]) extends ASImpl[F] {
  override def empty[A]: AsyncStream[F, A] = AsyncStream(ze.zero)

  override def collectLeft[A, B](s: AsyncStream[F, A])(init: B)(f: (B, A) => B): F[B] = {
    def impl(d: F[Step[A, AsyncStream[F, A]]], acc: F[B]): F[B] =
      d.flatMap(step => impl(step.rest.data, acc.map(b => f(b, step.value)))).handleErrorWith(_ => acc)

    impl(s.data, init.pure[F])
  }

  override def fromIterable[T](it: Iterable[T]): AsyncStream[F, T] = AsyncStream {
    if (it.nonEmpty) Step(it.head, fromIterable(it.tail)).pure[F] else ze.zero
  }

  override def takeWhile[T](s: AsyncStream[F, T])(p: (T) => Boolean): AsyncStream[F, T] = AsyncStream {
    s.data.flatMap {
      case step if !p(step.value) => ze.zero
      case step => Step(step.value, takeWhile(step.rest)(p)).pure[F]
    }
  }

  override def isEmpty[T](s: AsyncStream[F, T]): F[Boolean] = s.data.map(_ => false).handleErrorWith(_ => true.pure[F])
}