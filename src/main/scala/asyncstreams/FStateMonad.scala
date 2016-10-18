package asyncstreams

import scala.concurrent.{ExecutionContext, Future}
import scalaz.MonadPlus

class FStateMonad[S](implicit ex: ExecutionContext)
  extends MonadPlus[({ type f[X] = FState[S, X]})#f] with FStateMonadFunctions {
  type F[X] = FState[S, X]

  override def empty[A]: F[A] = FState((s: S) => ENDF)

  override def point[A](a: => A): F[A] = FState.unit(a)

  override def bind[A, B](m: F[A])(f: A => F[B]): F[B] =
    FState((s: S) => m(s) flatMap {
      case null => ENDF
      case (fst, snd) => f(fst)(snd)
    })

  override def plus[A](a: F[A],b: => F[A]): F[A] = bind(a)(_ => b)

  def conds(f: S => Boolean): F[Boolean] = bind(gets[S])(vs => point(f(vs)))
  def fconds(f: S => F[Boolean]): F[Boolean] = bind(gets[S])(f)
  def mods(f: S => S): F[S] = bind(gets[S])(vs => puts(f(vs)))

  def forM_[A](cond: S => Boolean, mod: S => S)(action: => F[A]): F[Unit] =
    whileM_(conds(cond), bind(action)(va => mods(mod)))
}

trait FStateMonadFunctions {
  def gets[S](implicit ex: ExecutionContext): FState[S, S] = FState((s: S) => Future((s, s)))
  def puts[S](news: S)(implicit ex: ExecutionContext): FState[S, S] = FState((_: S) => Future((news, news)))

  def conds[S](f: S => Boolean)(implicit m: FStateMonad[S]): FState[S, Boolean] =
    m.conds(f)

  def fconds[S](f: S => FState[S, Boolean])(implicit m: FStateMonad[S]): FState[S, Boolean] =
    m.fconds(f)

  def mods[S : FStateMonad](f: S => S)(implicit m: FStateMonad[S]): FState[S, S] =
    m.mods(f)
}
