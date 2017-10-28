package asyncstreams

import asyncstreams.typeclass.ZeroK
import cats.{Alternative, MonadError}

import scala.language.higherKinds

/**
  * This class doesn't fully implement Alternative
  * it is usable only for filtering
  */
class AlternativeForMonadError[F[+_]](implicit fme: MonadError[F, Throwable], zk: ZeroK[F]) extends Alternative[F] {
  override def ap[A, B](ff: F[A => B])(fa: F[A]) = fme.ap(ff)(fa)

  override def empty[A] = zk.zero[A]

  override def combineK[A](x: F[A], y: F[A]) = ???

  override def pure[A](x: A) = fme.pure(x)
}
