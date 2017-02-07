package asyncstreams

import scala.language.higherKinds
import scalaz.{Monad, MonadPlus}
import scalaz.syntax.monad._

class FStateMonad[Fu[+_]: Monad, S] extends MonadPlus[FState[Fu, S, ?]] with FStateMonadFunctions {
  type FS[X] = FState[Fu, S, X]

  override def empty[A]: FS[A] = FState.empty[Fu, S, A]

  override def point[A](a: => A): FS[A] = FState.unit(a)

  override def bind[A, B](m: FS[A])(f: A => FS[B]): FS[B] = m.bind(f)

  override def plus[A](a: FS[A], b: => FS[A]): FS[A] = bind(a)(_ => b)

  def condS(f: S => Boolean): FS[Boolean] = bind(getS[Fu, S])(vs => point(f(vs)))
  def fcondS(f: S => FS[Boolean]): FS[Boolean] = bind(getS[Fu, S])(f)
  def modS(f: S => S): FS[S] = bind(getS[Fu, S])(vs => putS(f(vs)))

  def forM_[A](cond: S => Boolean, mod: S => S)(action: => FS[A]): FS[Unit] =
    whileM_(condS(cond), bind(action)(va => modS(mod)))
}

trait FStateMonadFunctions {
  def getS[Fu[+_]: Monad, S]: FState[Fu, S, S] = FState((s: S) => (s, s).point[Fu])

  def putS[Fu[+_]: Monad, S](news: S): FState[Fu, S, S] = FState((_: S) => (news, news).point[Fu])

  def condS[Fu[+_]: Monad, S](f: S => Boolean)(implicit m: FStateMonad[Fu, S]): FStateMonad[Fu, S]#FS[Boolean] = m.condS(f)

  def fconds[Fu[+_]: Monad, S](f: S => FState[Fu, S, Boolean])(implicit m: FStateMonad[Fu, S]): FStateMonad[Fu, S]#FS[Boolean] = m.fconds(f)

  def modS[Fu[+_]: Monad, S](f: S => S)(implicit m: FStateMonad[Fu, S]): FStateMonad[Fu, S]#FS[S] = m.modS(f)
}