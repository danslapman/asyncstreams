package asyncstreams.typeclass

import scala.language.higherKinds

trait EmptyK[F[_]] {
  def empty[A]: F[A]
}
