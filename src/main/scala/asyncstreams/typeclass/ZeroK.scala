package asyncstreams.typeclass

import scala.language.higherKinds

trait ZeroK[F[+_]] {
  def zero[A]: F[A]
}
