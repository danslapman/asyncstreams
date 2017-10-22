package asyncstreams

import asyncstreams.typeclass.ZeroK

import scala.language.higherKinds
import scalaz.{Monad, MonadError, MonadPlus}

object Implicits {
  object MonadErrorInstances {
    implicit def streamMonadPlus[F[+_]: λ[`x[+_]` => MonadError[x, Throwable]]]: MonadPlus[AsyncStream[F, ?]] = new ASMonadPlusForMonadError[F]
    implicit def impl[F[+_]: λ[`x[+_]` => MonadError[x, Throwable]]]: ASImpl[F] = new ASImplForMonadError[F]
    implicit def zeroK[F[+_]](implicit me: MonadError[F, Throwable]): ZeroK[F] = new ZeroK[F] {
      override def zero[A] = me.raiseError(new NoSuchElementException)
    }
  }

  def asStateTOps[F[+_]: Monad](implicit methods: ASImpl[F]) = new ASStateTOps[F]
}
