import cats.mtl.FunctorEmpty
import cats.mtl.syntax.empty._

import scala.language.higherKinds

package object asyncstreams {
  implicit class FunctorWithFilter[F[_] : FunctorEmpty, A](fa: F[A]) {
    def withFilter(f: A ⇒ Boolean): F[A] = fa.filter(f)
  }
}
