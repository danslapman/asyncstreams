package asyncstreams

import scala.concurrent.{ExecutionContext, Future}

class FState[S, A](val func: S => Future[(A, S)]) {
  import FState._

  def apply(s: S) = func(s)

  def flatMap[B](f: A => FState[S, B])(implicit ex: ExecutionContext): FState[S, B] = FState[S, B](
    (s: S) => func(s).flatMap {
      case END => ENDF
      case (fst, snd) => f(fst)(snd)
    }
  )

  def map[B](f: A => B)(implicit ex: ExecutionContext): FState[S, B] =
    flatMap((a: A) => FState.unit(f(a)))

  def bind[B](f: A => FState[S, B])(implicit ex: ExecutionContext): FState[S, B] =
    FState((s: S) => func(s) flatMap {
      case END => ENDF
      case (fst, snd) => f(fst)(snd)
    })

  def filter(p: A => Boolean)(implicit executor: ExecutionContext): FState[S, A] =
    bind(a => if (p(a)) unit(a) else empty[S, A])

  def withFilter(p: A => Boolean)(implicit executor: ExecutionContext): FState[S, A] = filter(p)
}

object FState {
  def apply[S, A](f: S => Future[(A, S)]) = new FState[S, A](f)
  def unit[S, A](a: => A)(implicit ex: ExecutionContext) = FState[S, A]((s: S) => Future((a, s)))
  def empty[S, A](implicit ex: ExecutionContext) = FState[S, A]((s: S) => ENDF)
}
