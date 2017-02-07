package asyncstreams

import scala.language.higherKinds
import scalaz.{Monad, MonadPlus}
import scalaz.syntax.monad._

class FStateMonad[Fu[+_]: Monad, S] extends FStateMonadFunctions[Fu] with MonadPlus[FState[Fu, S, ?]] {
  type FS[X] = FState[Fu, S, X]

  override def empty[A]: FS[A] = FState.empty[Fu, S, A]

  override def point[A](a: => A): FS[A] = FState.unit(a)

  override def bind[A, B](m: FS[A])(f: A => FS[B]): FS[B] = m.bind(f)

  override def plus[A](a: FS[A], b: => FS[A]): FS[A] = bind(a)(_ => b)

  def condS(f: S => Boolean): FS[Boolean] = bind(getS[S])(vs => point(f(vs)))
  def fcondS(f: S => FS[Boolean]): FS[Boolean] = bind(getS[S])(f)
  def modS(f: S => S): FS[S] = bind(getS[S])(vs => putS(f(vs)))

  def forM_[A](cond: S => Boolean, mod: S => S)(action: => FS[A]): FS[Unit] =
    whileM_(condS(cond), bind(action)(va => modS(mod)))
}

class FStateMonadFunctions[Fu[+_]: Monad] {
  def getS[S]: FState[Fu, S, S] = FState((s: S) => (s, s).point[Fu])

  def putS[S](news: S): FState[Fu, S, S] = FState((_: S) => (news, news).point[Fu])

  def condS[S](f: S => Boolean)(implicit m: FStateMonad[Fu, S]): FStateMonad[Fu, S]#FS[Boolean] = m.condS(f)

  def fconds[S](f: S => FState[Fu, S, Boolean])(implicit m: FStateMonad[Fu, S]): FStateMonad[Fu, S]#FS[Boolean] = m.fconds(f)

  def modS[S](f: S => S)(implicit m: FStateMonad[Fu, S]): FStateMonad[Fu, S]#FS[S] = m.modS(f)
}