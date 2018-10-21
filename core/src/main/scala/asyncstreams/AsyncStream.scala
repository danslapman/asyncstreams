package asyncstreams

import cats.kernel.Monoid
import cats.{Eval, Monad}
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import cats.syntax.semigroup._
import EmptyKOrElse.ops._

import scala.annotation.unchecked.{uncheckedVariance => uV}
import scala.collection.GenIterable
import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds

class AsyncStream[F[_]: Monad: EmptyKOrElse, A](private[asyncstreams] val data: F[Step[A, AsyncStream[F, A]]]) {
  private val EOS = EmptyKOrElse[F]

  def foldLeft[B](init: B)(f: (B, A) => B): F[B] = {
    def impl(d: F[Step[A, AsyncStream[F, A]]], acc: F[B]): F[B] =
      d.flatMap(step => impl(step._2.value.data, acc.map(b => f(b, step._1)))).orElse(acc)

    impl(data, init.pure[F])
  }

  def ++(other: AsyncStream[F, A]): AsyncStream[F, A] = AsyncStream {
    this.data.map(step => step._1 -> step._2.map(_ ++ other)).orElse(other.data)
  }

  def to[Col[+_]](implicit cbf: CanBuildFrom[Nothing, A, Col[A @uV]]): F[Col[A]] =
    foldLeft(cbf())((col, el) => col += el).map(_.result())

  def takeWhile(p: A => Boolean): AsyncStream[F, A] = AsyncStream {
    data.flatMap {
      case step if !p(step._1) => EOS.empty
      case step => (step._1 -> step._2.map(_.takeWhile(p))).pure[F]
    }
  }

  def take(n: Int): AsyncStream[F, A] =
    if (n <= 0) AsyncStream.empty
    else AsyncStream {
      data.map(p => p._1 -> p._2.map(_.take(n - 1)))
    }

  def drop(n: Int): AsyncStream[F, A] =
    if (n <= 0) this
    else AsyncStream {
      data.flatMap(p => p._2.value.drop(n - 1).data)
    }

  def foreach[U](f: A => U): F[Unit] =
    foldLeft(())((_: Unit, a: A) => {f(a); ()})

  def foreachF[U](f: A => F[U]): F[Unit] =
    foldLeft(().pure[F])((fu: F[Unit], a: A) => fu.flatMap(_ => f(a)).map(_ => ())).flatMap(identity)

  def flatten[B](implicit asIterable: A => GenIterable[B]): AsyncStream[F, B] = {
    def streamChunk(step: Step[A, AsyncStream[F, A]]): AsyncStream[F, B] =
       AsyncStream.fromIterable(asIterable(step._1).seq) ++ step._2.value.flatten

    AsyncStream(data.flatMap(step => streamChunk(step).data))
  }

  def isEmpty: F[Boolean] = data.map(_ => false).orElse(true.pure[F])
  def nonEmpty: F[Boolean] = isEmpty.map(!_)

  def map[B](f: A => B): AsyncStream[F, B] = AsyncStream {
    data.map(s => f(s._1) -> s._2.map(_.map(f)))
  }

  def mapF[B](f: A => F[B]): AsyncStream[F, B] = AsyncStream {
    data.flatMap(s => f(s._1).map(nv => nv -> s._2.map(_.mapF(f))))
  }

  def flatMap[B](f: A => AsyncStream[F, B]): AsyncStream[F, B] = AsyncStream {
    data.flatMap(s => (f(s._1) ++ s._2.value.flatMap(f)).data)
  }

  def filter(p: A => Boolean): AsyncStream[F, A] = AsyncStream {
    data.flatMap { s =>
      if (p(s._1)) (s._1 -> s._2.map(_.filter(p))).pure[F]
      else s._2.value.filter(p).data
    }
  }

  def withFilter(p: A => Boolean): AsyncStream[F, A] = filter(p)

  def find(p: A => Boolean): F[Option[A]] = {
    data.flatMap { s =>
      if (p(s._1)) s._1.some.pure[F]
      else s._2.value.find(p)
    }.orElse(none[A].pure[F])
  }

  def findF(p: A => F[Boolean]): F[Option[A]] = {
    data.flatMap { s =>
      p(s._1).flatMap {
        case true => s._1.some.pure[F]
        case false => s._2.value.findF(p)
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
    } yield (stepA._1, stepB._1) -> stepA._2.flatMap(ra => stepB._2.map(rb => ra zip rb))
  }

  def zipWithIndex: AsyncStream[F, (A, Int)] =
    zip(AsyncStream.unfold(0)(_ + 1))
}

object AsyncStream {
  private[asyncstreams] def apply[F[_]: Monad: EmptyKOrElse, A](data: => F[Step[A, AsyncStream[F, A]]]): AsyncStream[F, A] = new AsyncStream(data)
  def asyncNil[F[_]: Monad: EmptyKOrElse, A]: AsyncStream[F, A] = empty

  private[asyncstreams] def generate[F[_]: Monad: EmptyKOrElse, S, A](start: S)(gen: S => F[(S, A)]): AsyncStream[F, A] = AsyncStream {
    gen(start).map((stateEl: (S, A)) => stateEl._2 -> Eval.later(generate(stateEl._1)(gen)))
  }

  def empty[F[_]: Monad: EmptyKOrElse, A]: AsyncStream[F, A] = AsyncStream(EmptyKOrElse[F].empty)

  def fromIterable[F[_]: Monad: EmptyKOrElse, T](it: Iterable[T]): AsyncStream[F, T] = AsyncStream {
    if (it.nonEmpty) (it.head -> Eval.later(fromIterable(it.tail))).pure[F] else EmptyKOrElse[F].empty
  }

  def unfold[F[_]: Monad: EmptyKOrElse, T](start: T)(makeNext: T => T): AsyncStream[F, T] =
    generate(start)(s => (makeNext(s), s).pure[F])

  def unfoldM[F[_]: Monad: EmptyKOrElse, T](start: T)(makeNext: T => F[T]): AsyncStream[F, T] =
    generate(start)(s => makeNext(s).map(n => (n, s)))

  def unfoldMM[F[_]: Monad: EmptyKOrElse, T](start: F[T])(makeNext: T => F[T]): AsyncStream[F, T] = AsyncStream {
    start.flatMap(initial => generate(initial)(s => makeNext(s).map(n => (n, s))).data)
  }
}