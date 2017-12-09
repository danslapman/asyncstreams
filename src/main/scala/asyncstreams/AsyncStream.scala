package asyncstreams

import cats.{Alternative, Monad}
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.semigroupk._

import scala.annotation.tailrec
import scala.annotation.unchecked.{uncheckedVariance => uV}
import scala.collection.GenIterable
import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds

class AsyncStream[F[+_]: Monad, +A](private[asyncstreams] val data: F[Step[A, AsyncStream[F, A]]]) {
  def to[Col[+_]](implicit cbf: CanBuildFrom[Nothing, A, Col[A @uV]], methods: ASImpl[F]): F[Col[A]] =
    methods.collectLeft(this)(cbf())((col, el) => col += el).map(_.result())

  def takeWhile(p: A => Boolean)(implicit impl: ASImpl[F]): AsyncStream[F, A] = impl.takeWhile(this)(p)

  def take(n: Int)(implicit smp: Alternative[AsyncStream[F, ?]]): AsyncStream[F, A] =
    if (n <= 0) smp.empty
    else AsyncStream {
      data.map(p => Step(p.value, p.rest.take(n - 1)))
    }

  def foreach[U](f: (A) => U)(implicit methods: ASImpl[F]): F[Unit] =
    methods.collectLeft(this)(())((_: Unit, a: A) => {f(a); ()})

  def foreachF[U](f: (A) => F[U])(implicit impl: ASImpl[F]): F[Unit] =
    impl.collectLeft(this)(().pure[F])((fu: F[Unit], a: A) => fu.flatMap(_ => f(a)).map(_ => ())).flatMap(identity)

  def flatten[B](implicit asIterable: A => GenIterable[B], smp: Alternative[AsyncStream[F, ?]], impl: ASImpl[F]): AsyncStream[F, B] = {
    def streamChunk(step: Step[A, AsyncStream[F, A]]): AsyncStream[F, B] =
      impl.fromIterable(asIterable(step.value).seq) <+> step.rest.flatten

    AsyncStream(data.flatMap(step => streamChunk(step).data))
  }

  def isEmpty(implicit impl: ASImpl[F]): F[Boolean] = impl.isEmpty(this)
  def nonEmpty(implicit impl: ASImpl[F]): F[Boolean] = impl.isEmpty(this).map(!_)

  def map[B](f: A => B): AsyncStream[F, B] = AsyncStream {
    data.map(s => Step(f(s.value), s.rest.map(f)))
  }

  def mapF[B](f: A => F[B]): AsyncStream[F, B] = AsyncStream {
    data.flatMap(s => f(s.value).map(nv => Step(nv, s.rest.mapF(f))))
  }

  def flatMap[B](f: A => AsyncStream[F, B])(implicit smp: Alternative[AsyncStream[F, ?]]): AsyncStream[F, B] = AsyncStream {
    data.flatMap(s => (f(s.value) <+> s.rest.flatMap(f)).data)
  }

  def filter(p: A => Boolean): AsyncStream[F, A] = AsyncStream {
    data.flatMap { s =>
      if (p(s.value)) Step(s.value, s.rest.filter(p)).pure[F]
      else s.rest.filter(p).data
    }
  }
}

object AsyncStream {
  private[asyncstreams] def apply[F[+_]: Monad, A](data: => F[Step[A, AsyncStream[F, A]]]): AsyncStream[F, A] = new AsyncStream(data)
  def asyncNil[F[+_]: Monad, A](implicit impl: ASImpl[F]): AsyncStream[F, A] = impl.empty

  private[asyncstreams] def generate[F[+_]: Monad, S, A](start: S)(gen: S => F[(S, A)])(implicit smp: Alternative[AsyncStream[F, ?]]): AsyncStream[F, A] = AsyncStream {
    gen(start).map((stateEl: (S, A)) => Step(stateEl._2, generate(stateEl._1)(gen)))
  }

  def unfold[F[+_]: Monad, T](start: T)(makeNext: T => T)(implicit smp: Alternative[AsyncStream[F, ?]]): AsyncStream[F, T] =
    generate(start)(s => (makeNext(s), s).pure[F])

  def unfoldM[F[+_]: Monad, T](start: T)(makeNext: T => F[T])(implicit alt: Alternative[AsyncStream[F, ?]]): AsyncStream[F, T] =
    generate(start)(s => makeNext(s).map(n => (n, s)))

  def unfoldMM[F[+_]: Monad, T](start: F[T])(makeNext: T => F[T])(implicit alt: Alternative[AsyncStream[F, ?]]): AsyncStream[F, T] = AsyncStream {
    start.flatMap(initial => generate(initial)(s => makeNext(s).map(n => (n, s))).data)
  }
}