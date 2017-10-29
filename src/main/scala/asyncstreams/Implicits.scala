package asyncstreams

import alleycats.EmptyK
import cats.{Alternative, Monad, MonadError}

import scala.language.higherKinds

object Implicits {
  object MonadErrorInstances {
    implicit def streamInstance[F[+_]: λ[`x[+_]` => MonadError[x, Throwable]]]: Monad[AsyncStream[F, ?]] with Alternative[AsyncStream[F, ?]] = new ASInstanceForMonadError[F]
    implicit def impl[F[+_]: λ[`x[+_]` => MonadError[x, Throwable]]]: ASImpl[F] = new ASImplForMonadError[F]
    implicit def zeroK[F[+_]](implicit me: MonadError[F, Throwable]): EmptyK[F] = new EmptyK[F] {
      override def empty[A]: F[A] = me.raiseError(new NoSuchElementException)
    }
  }

  def asStateTOps[F[+_]: Monad](implicit methods: ASImpl[F]) = new ASStateTOps[F]
}
