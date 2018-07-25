package asyncstreams

import alleycats.EmptyK
import simulacrum.typeclass

import scala.language.higherKinds

@typeclass trait EmptyKOrElse[F[_]] extends EmptyK[F] {
  def orElse[A](default: => F[A]): F[A]
}
