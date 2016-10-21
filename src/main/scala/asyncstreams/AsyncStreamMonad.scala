package asyncstreams

import scala.concurrent.{ExecutionContext, Future}
import scalaz.MonadPlus

class AsyncStreamMonad(implicit executor: ExecutionContext) extends MonadPlus[AsyncStream] {
  import AsyncStream._

  override def empty[A] = nil[A]

  override def point[A](a: => A): AsyncStream[A] = single(a)

  override def plus[A](a: AsyncStream[A], b: => AsyncStream[A]) = concat(a, b)

  override def bind[A, B](ma: AsyncStream[A])(f: A => AsyncStream[B]): AsyncStream[B] =
    AsyncStream(
      ma.data.flatMap {
        case END => ENDF
        case step => f(step.value).data.map { step2 =>
          Step(step2.value, concat(step2.rest, bind(step.rest)(f)))
        }
      }
    )
}

trait AsyncStreamMonadFunctions {
  def foreach[A, S](stream: AsyncStream[A])(f: A => FState[S, _])
    (implicit ex: ExecutionContext): FState[S, Unit] =
    FState(s => {
      stream.foldLeft(Future(s))((futureS, a) => futureS.flatMap(s2 => f(a)(s2).map(_._2)))
        .flatMap(identity).map(((), _))
    })

  def isEmpty[A, S](stream: AsyncStream[A])(implicit ex: ExecutionContext): FState[S, Boolean] =
    FState(s => stream.data.map(step => (step eq END, s)))

  def isEmpty[A, S : FStateMonad](f: S => AsyncStream[A])(implicit fsm: FStateMonad[S], ex: ExecutionContext): FState[S, Boolean] =
    fsm.fcondS((s: S) => isEmpty(f(s)))

  def notEmpty[A, S](stream: AsyncStream[A])(implicit ex: ExecutionContext): FState[S, Boolean] =
    FState(s => stream.data map (step => (!(step eq END), s)))

  def notEmpty[A, S](f: S => AsyncStream[A])(implicit fsm: FStateMonad[S], ex: ExecutionContext): FState[S, Boolean] =
    fsm.fcondS(s => notEmpty(f(s)))

  def get[A, S](stream: AsyncStream[A])(implicit ex: ExecutionContext): FState[S, (A, AsyncStream[A])] =
    FState(s => stream.data.map(step => ((step.value, step.rest), s)))

  def generateS[S,A](start: S)(gen: FState[S, A])(implicit ex: ExecutionContext) =
    AsyncStream.generate(start)(gen.func)
}
