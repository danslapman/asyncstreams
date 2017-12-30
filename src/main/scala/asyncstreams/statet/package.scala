package asyncstreams

import alleycats.EmptyK
import cats.data.StateT
import cats.mtl.FunctorEmpty
import cats.{Functor, Monad}

import scala.language.higherKinds

package object statet {
  implicit def stateTFunctorEmpty[F[+_], S](implicit functor: Functor[StateT[F, S, ?]], mf: Monad[F], emptyk: EmptyK[F]): FunctorEmpty[StateT[F, S, ?]] =
    new StateTFunctorEmpty[F, S]()

  def asStateTOps[F[+_]: Monad](implicit impl: ASImpl[F]) = new ASStateTOps[F]
}
