package asyncstreams

import alleycats.EmptyK
import cats.{Alternative, Monad, MonadError}

import scala.language.higherKinds

package object impl {
  implicit def streamInstance[F[+_]: λ[`x[+_]` => MonadError[x, Throwable]]]: Monad[AsyncStream[F, ?]] with Alternative[AsyncStream[F, ?]] = new ASInstanceForMonadError[F]
  implicit def asimpl[F[+_]: λ[`x[+_]` => MonadError[x, Throwable]]]: ASImpl[F] = new ASImplForMonadError[F]
  implicit def zeroK[F[+_]](implicit me: MonadError[F, Throwable]): EmptyK[F] = new EmptyK[F] {
    override def empty[A]: F[A] = me.raiseError(new NoSuchElementException)
  }
}
