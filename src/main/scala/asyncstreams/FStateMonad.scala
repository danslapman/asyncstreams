package asyncstreams

import scala.concurrent.{ExecutionContext, Future}
import scalaz.MonadPlus

class FStateMonad[S](implicit ex: ExecutionContext)
  extends MonadPlus[FState[S, ?]] with FStateMonadFunctions {
  type F[X] = FState[S, X]

  override def empty[A]: F[A] = FState.empty[S, A]

  override def point[A](a: => A): F[A] = FState.unit(a)

  override def bind[A, B](m: F[A])(f: A => F[B]): F[B] = m.bind(f)

  override def plus[A](a: F[A],b: => F[A]): F[A] = bind(a)(_ => b)

  def condS(f: S => Boolean): F[Boolean] = bind(getS[S])(vs => point(f(vs)))
  def fcondS(f: S => F[Boolean]): F[Boolean] = bind(getS[S])(f)
  def modS(f: S => S): F[S] = bind(getS[S])(vs => putS(f(vs)))

  def forM_[A](cond: S => Boolean, mod: S => S)(action: => F[A]): F[Unit] =
    whileM_(condS(cond), bind(action)(va => modS(mod)))
}

trait FStateMonadFunctions {
  def getS[S](implicit ex: ExecutionContext): FState[S, S] = FState((s: S) => Future((s, s)))

  def putS[S](news: S)(implicit ex: ExecutionContext): FState[S, S] = FState((_: S) => Future((news, news)))

  def condS[S](f: S => Boolean)(implicit m: FStateMonad[S]): FStateMonad[S]#F[Boolean] = m.condS(f)

  def fconds[S](f: S => FState[S, Boolean])(implicit m: FStateMonad[S]): FStateMonad[S]#F[Boolean] = m.fconds(f)

  def modS[S](f: S => S)(implicit m: FStateMonad[S]): FStateMonad[S]#F[S] = m.modS(f)
}