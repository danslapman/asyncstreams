package asyncstreams

import scala.annotation.unchecked.{uncheckedVariance => uV}
import scala.collection.GenIterable
import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds
import scalaz.syntax.monadPlus._
import scalaz.{Monad, MonadPlus}

class AsyncStream[F[+_]: Monad, A](val data: F[Step[A, AsyncStream[F, A]]]) {
  type SStep = Step[A, AsyncStream[F, A]]

  def to[Col[_]](implicit cbf: CanBuildFrom[Nothing, A, Col[A @uV]], methods: ASImpl[F]): F[Col[A]] =
    methods.collectLeft(this)(cbf())((col, el) => col += el).map(_.result())

  def takeWhile(p: A => Boolean)(implicit impl: ASImpl[F]): AsyncStream[F, A] = impl.takeWhile(this)(p)

  def take(n: Int)(implicit smp: MonadPlus[AsyncStream[F, ?]]): AsyncStream[F, A] =
    if (n <= 0) smp.empty
    else AsyncStream {
      data.map(p => Step(p.value, p.rest.take(n - 1)))
    }

  def foreach[U](f: (A) => U)(implicit methods: ASImpl[F]): F[Unit] =
    methods.collectLeft(this)(())((_: Unit, a: A) => {f(a); ()})

  def foreachF[U](f: (A) => F[U])(implicit impl: ASImpl[F]): F[Unit] =
    impl.collectLeft(this)(().point[F])((fu: F[Unit], a: A) => fu.flatMap(_ => f(a)).map(_ => ())).flatMap(identity)

  def flatten[B](implicit asIterable: A => GenIterable[B], smp: MonadPlus[AsyncStream[F, ?]], impl: ASImpl[F]): AsyncStream[F, B] = {
    def streamChunk(step: AsyncStream[F, A]#SStep): AsyncStream[F, B] =
      impl.fromIterable(asIterable(step.value).seq) <+> step.rest.flatten

    AsyncStream(data.flatMap(step => streamChunk(step).data))
  }

  def isEmpty(implicit impl: ASImpl[F]): F[Boolean] = impl.isEmpty(this)
  def nonEmpty(implicit impl: ASImpl[F]): F[Boolean] = impl.isEmpty(this).map(!_)
}

object AsyncStream {
  def apply[F[+_]: Monad, A](data: => F[Step[A, AsyncStream[F, A]]]): AsyncStream[F, A] = new AsyncStream(data)
  def asyncNil[F[+_]: Monad, A](implicit impl: ASImpl[F]): AsyncStream[F, A] = impl.empty

  private[asyncstreams] def generate[F[+_]: Monad, S, A](start: S)(gen: S => F[(S, A)])(implicit smp: MonadPlus[AsyncStream[F, ?]]): AsyncStream[F, A] = AsyncStream {
    gen(start).map((stateEl: (S, A)) => Step(stateEl._2, generate(stateEl._1)(gen)))
  }

  def unfold[F[+_]: Monad, T](start: T)(makeNext: T => T)(implicit smp: MonadPlus[AsyncStream[F, ?]]): AsyncStream[F, T] =
    generate(start)(s => (makeNext(s), s).point[F])

  implicit class AsyncStreamOps[F[+_]: Monad, A](stream: => AsyncStream[F, A]) {
    def ~::(el: A) = AsyncStream(Step(el, stream).point[F])
  }
}