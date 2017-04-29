package asyncstreams

import scala.language.higherKinds
import scalaz.syntax.monadError._
import scalaz.{MonadError, MonadPlus}

/**
  * This class doesn't fully implement MonadPlus
  * it is usable only for filtering
  */
class MonadFilterForMonadError[F[_]](implicit fmp: MonadError[F, Throwable], ze: ZeroError[Throwable, F]) extends MonadPlus[F] {
  override def point[A](a: => A): F[A] = fmp.point(a)

  override def empty[A]: F[A] = ze.zeroElement.raiseError

  override def bind[A, B](fa: F[A])(f: (A) => F[B]): F[B] = fmp.bind(fa)(f)

  override def plus[A](a: F[A], b: => F[A]): F[A] = ???
}
