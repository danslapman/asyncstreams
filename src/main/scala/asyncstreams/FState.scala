package asyncstreams

import scala.language.higherKinds
import scalaz.Monad
import scalaz.syntax.monad._

class FState[F[+_]: Monad, S, A](val func: S => F[(A, S)]) {
  import FState._

  def apply(s: S): F[(A, S)] = func(s)

  def flatMap[B](f: A => FState[F, S, B]): FState[F, S, B] = FState[F, S, B](
    (s: S) => func(s).flatMap {
      case END => ENDF[F]
      case (fst, snd) => f(fst)(snd)
    }
  )

  def map[B](f: A => B): FState[F, S, B] = flatMap((a: A) => FState.unit(f(a)))

  def bind[B](f: A => FState[F, S, B]): FState[F, S, B] =
    FState((s: S) => func(s) flatMap {
      case END => ENDF[F]
      case (fst, snd) => f(fst)(snd)
    })

  def filter(p: A => Boolean): FState[F, S, A] =
    bind(a => if (p(a)) unit(a) else empty[F, S, A])

  def withFilter(p: A => Boolean): FState[F, S, A] = filter(p)
}

object FState {
  def apply[F[+_]: Monad, S, A](f: S => F[(A, S)]) = new FState[F, S, A](f)
  def unit[F[+_]: Monad, S, A](a: => A) = FState[F, S, A]((s: S) => (a, s).point[F])
  def empty[F[+_]: Monad, S, A] = FState[F, S, A]((s: S) => ENDF[F])
}
