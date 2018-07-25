package asyncstreams

import alleycats.EmptyK
import cats.data.StateT
import cats.mtl.FunctorEmpty
import cats.{Functor, Monad}

import scala.language.higherKinds

package object statet {
  def asStateTOps[F[_]: Monad: EmptyKOrElse] = new ASStateTOps[F]
}
