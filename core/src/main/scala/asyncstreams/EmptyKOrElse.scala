package asyncstreams

import alleycats.EmptyK
import simulacrum.{op, typeclass}

import scala.language.higherKinds

@typeclass trait EmptyKOrElse[F[_]] extends EmptyK[F] {
  @op("orElse") def orElse[A](fa: F[A], default: => F[A]): F[A]
}
