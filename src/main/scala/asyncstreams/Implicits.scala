package asyncstreams

import alleycats.EmptyK
import cats.data.StateT
import cats.mtl.FunctorEmpty
import cats.{Alternative, Functor, Monad, MonadError}

import scala.language.higherKinds

object Implicits {
  object MonadErrorInstances {
    implicit def streamInstance[F[+_]: λ[`x[+_]` => MonadError[x, Throwable]]]: Monad[AsyncStream[F, ?]] with Alternative[AsyncStream[F, ?]] = new ASInstanceForMonadError[F]
    implicit def impl[F[+_]: λ[`x[+_]` => MonadError[x, Throwable]]]: ASImpl[F] = new ASImplForMonadError[F]
    implicit def zeroK[F[+_]](implicit me: MonadError[F, Throwable]): EmptyK[F] = new EmptyK[F] {
      override def empty[A]: F[A] = me.raiseError(new NoSuchElementException)
    }
  }

  implicit def stateTFunctorEmpty[F[+_], S](implicit functor: Functor[StateT[F, S, ?]], mf: Monad[F], emptyk: EmptyK[F]): FunctorEmpty[StateT[F, S, ?]] =
    new StateTFunctorEmpty[F, S]()

  def asStateTOps[F[+_]: Monad](implicit methods: ASImpl[F]) = new ASStateTOps[F]
}
