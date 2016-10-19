package asyncstreams

import scala.concurrent.{ExecutionContext, Future}

class FState[S, A](val func: S => Future[(A, S)]) extends ((S) => Future[(A, S)]) {
  def apply(s: S) = func(s)

  def flatMap[B](f: A => FState[S, B])(implicit ex: ExecutionContext): FState[S, B] = FState[S, B](
    (s: S) => func(s) flatMap ((fst: A, snd: S) => f(fst)(snd)).tupled
  )

  def map[B](f: A => B)(implicit ex: ExecutionContext): FState[S, B] =
    flatMap((a: A) => FState.unit(f(a)))
}

object FState {
  def apply[S, A](f: S => Future[(A, S)]) = new FState[S, A](f)
  def unit[S, A](a: => A)(implicit ex: ExecutionContext) = FState[S, A]((s: S) => Future((a, s)))
}
