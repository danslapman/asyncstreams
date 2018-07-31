package asyncstreams

import cats.kernel.Monoid
import cats.{Alternative, Applicative, Monad}
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import cats.syntax.semigroup._
import cats.syntax.semigroupk._
import EmptyKOrElse.ops._

import scala.annotation.unchecked.{uncheckedVariance => uV}
import scala.collection.GenIterable
import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds

class AsyncStream[F[_]: Monad: EmptyKOrElse, A](private[asyncstreams] val data: F[Step[A, AsyncStream[F, A]]]) {
  private val EOS = EmptyKOrElse[F]

  def foldLeft[B](init: B)(f: (B, A) => B): F[B] = {
    def impl(d: F[Step[A, AsyncStream[F, A]]], acc: F[B]): F[B] =
      d.flatMap(step => impl(step.rest.data, acc.map(b => f(b, step.value)))).orElse(acc)

    impl(data, init.pure[F])
  }

  def to[Col[+_]](implicit cbf: CanBuildFrom[Nothing, A, Col[A @uV]]): F[Col[A]] =
    foldLeft(cbf())((col, el) => col += el).map(_.result())

  def takeWhile(p: A => Boolean): AsyncStream[F, A] = AsyncStream {
    data.flatMap {
      case step if !p(step.value) => EOS.empty
      case step => Step(step.value, step.rest.takeWhile(p)).pure[F]
    }
  }

  def take(n: Int): AsyncStream[F, A] =
    if (n <= 0) AsyncStream.empty
    else AsyncStream {
      data.map(p => Step(p.value, p.rest.take(n - 1)))
    }

  def drop(n: Int): AsyncStream[F, A] =
    if (n <= 0) this
    else AsyncStream {
      data.flatMap(p => p.rest.drop(n - 1).data)
    }

  def foreach[U](f: A => U): F[Unit] =
    foldLeft(())((_: Unit, a: A) => {f(a); ()})

  def foreachF[U](f: A => F[U]): F[Unit] =
    foldLeft(().pure[F])((fu: F[Unit], a: A) => fu.flatMap(_ => f(a)).map(_ => ())).flatMap(identity)

  def flatten[B](implicit asIterable: A => GenIterable[B], alt: Alternative[AsyncStream[F, ?]]): AsyncStream[F, B] = {
    def streamChunk(step: Step[A, AsyncStream[F, A]]): AsyncStream[F, B] =
       AsyncStream.fromIterable(asIterable(step.value).seq) <+> step.rest.flatten

    AsyncStream(data.flatMap(step => streamChunk(step).data))
  }

  def isEmpty: F[Boolean] = data.map(_ => false).orElse(true.pure[F])
  def nonEmpty: F[Boolean] = isEmpty.map(!_)

  def map[B](f: A => B): AsyncStream[F, B] = AsyncStream {
    data.map(s => Step(f(s.value), s.rest.map(f)))
  }

  def mapF[B](f: A => F[B]): AsyncStream[F, B] = AsyncStream {
    data.flatMap(s => f(s.value).map(nv => Step(nv, s.rest.mapF(f))))
  }

  def flatMap[B](f: A => AsyncStream[F, B])(implicit alt: Alternative[AsyncStream[F, ?]]): AsyncStream[F, B] = AsyncStream {
    data.flatMap(s => (f(s.value) <+> s.rest.flatMap(f)).data)
  }

  def filter(p: A => Boolean): AsyncStream[F, A] = AsyncStream {
    data.flatMap { s =>
      if (p(s.value)) Step(s.value, s.rest.filter(p)).pure[F]
      else s.rest.filter(p).data
    }
  }

  def withFilter(p: A => Boolean): AsyncStream[F, A] = filter(p)

  def find(p: A => Boolean): F[Option[A]] = {
    data.flatMap { s =>
      if (p(s.value)) s.value.some.pure[F]
      else s.rest.find(p)
    }.orElse(none[A].pure[F])
  }

  def findF(p: A => F[Boolean]): F[Option[A]] = {
    data.flatMap { s =>
      p(s.value).flatMap {
        case true => s.value.some.pure[F]
        case false => s.rest.findF(p)
      }
    }.orElse(none[A].pure[F])
  }

  def partition(p: A => Boolean): (AsyncStream[F, A], AsyncStream[F, A]) = (filter(p), filter(p.andThen(!_)))

  def foldMap[B: Monoid](f: A => B): F[B] = {
    foldLeft(Monoid[B].empty)((b, a) => b |+| f(a))
  }

  def zip[B](sb: AsyncStream[F, B]): AsyncStream[F, (A, B)] = AsyncStream {
    for {
      stepA <- data
      stepB <- sb.data
    } yield Step((stepA.value, stepB.value), stepA.rest zip stepB.rest)
  }

  def zipWithIndex(implicit app: Applicative[AsyncStream[F, ?]]): AsyncStream[F, (A, Int)] =
    zip(AsyncStream.unfold(0)(_ + 1))
}

object AsyncStream {
  private[asyncstreams] def apply[F[_]: Monad: EmptyKOrElse, A](data: => F[Step[A, AsyncStream[F, A]]]): AsyncStream[F, A] = new AsyncStream(data)
  def asyncNil[F[_]: Monad: EmptyKOrElse, A]: AsyncStream[F, A] = empty

  private[asyncstreams] def generate[F[_]: Monad: EmptyKOrElse, S, A](start: S)(gen: S => F[(S, A)]): AsyncStream[F, A] = AsyncStream {
    gen(start).map((stateEl: (S, A)) => Step(stateEl._2, generate(stateEl._1)(gen)))
  }

  def empty[F[_]: Monad: EmptyKOrElse, A]: AsyncStream[F, A] = AsyncStream(EmptyKOrElse[F].empty)

  def fromIterable[F[_]: Monad: EmptyKOrElse, T](it: Iterable[T]): AsyncStream[F, T] = AsyncStream {
    if (it.nonEmpty) Step(it.head, fromIterable(it.tail)).pure[F] else EmptyKOrElse[F].empty
  }

  def unfold[F[_]: Monad: EmptyKOrElse, T](start: T)(makeNext: T => T)(implicit app: Applicative[AsyncStream[F, ?]]): AsyncStream[F, T] =
    generate(start)(s => (makeNext(s), s).pure[F])

  def unfoldM[F[_]: Monad: EmptyKOrElse, T](start: T)(makeNext: T => F[T])(implicit app: Applicative[AsyncStream[F, ?]]): AsyncStream[F, T] =
    generate(start)(s => makeNext(s).map(n => (n, s)))

  def unfoldMM[F[_]: Monad: EmptyKOrElse, T](start: F[T])(makeNext: T => F[T])(implicit app: Applicative[AsyncStream[F, ?]]): AsyncStream[F, T] = AsyncStream {
    start.flatMap(initial => generate(initial)(s => makeNext(s).map(n => (n, s))).data)
  }
}