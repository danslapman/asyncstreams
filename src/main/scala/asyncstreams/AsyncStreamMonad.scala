package asyncstreams

import scala.language.higherKinds
import scalaz.{Monad, MonadPlus}
import scalaz.syntax.monad._

class AsyncStreamMonad[F[+_]: Monad] extends MonadPlus[AsyncStream[F, ?]] {
  import AsyncStream._

  override def empty[A]: AsyncStream[F, A] = nil[F, A]

  override def point[A](a: => A): AsyncStream[F, A] = single(a)

  override def plus[A](a: AsyncStream[F, A], b: => AsyncStream[F, A]): AsyncStream[F, A] = concat(a, b)

  override def bind[A, B](ma: AsyncStream[F, A])(f: A => AsyncStream[F, B]): AsyncStream[F, B] =
    AsyncStream(
      ma.data.flatMap {
        case END => ENDF
        case step => f(step.value).data.map { step2 =>
          Step(step2.value, concat(step2.rest, bind(step.rest)(f)))
        }
      }
    )
}

class AsyncStreamMonadFunctions[F[+_]: Monad] {
  def foreach[A, S](stream: AsyncStream[F, A])(f: A => FState[F, S, _]): FState[F, S, Unit] =
    FState(s => {
      stream.foldLeft(s.point[F])((fS, a) => fS.flatMap(s2 => f(a)(s2).map(_._2)))
        .flatMap(identity).map(((), _))
    })

  def isEmpty[A, S](stream: AsyncStream[F, A]): FState[F, S, Boolean] =
    FState(s => stream.data.map(step => (step eq END, s)))

  def isEmpty[A, S](f: S => AsyncStream[F, A])(implicit fsm: FStateMonad[F, S]): FState[F, S, Boolean] =
    fsm.fcondS((s: S) => isEmpty(f(s)))

  def notEmpty[A, S](stream: AsyncStream[F, A]): FState[F, S, Boolean] =
    FState(s => stream.data map (step => (!(step eq END), s)))

  def notEmpty[A, S](f: S => AsyncStream[F, A])(implicit fsm: FStateMonad[F, S]): FState[F, S, Boolean] =
    fsm.fcondS(s => notEmpty(f(s)))

  def get[A, S](stream: AsyncStream[F, A]): FState[F, S, (A, AsyncStream[F, A])] =
    FState(s => stream.data.map(step => ((step.value, step.rest), s)))

  def generateS[S, A](start: S)(gen: FState[F, S, A]): AsyncStream[F, A] =
    AsyncStream.generate(start)(gen.func)
}
