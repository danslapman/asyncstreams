package asyncstreams

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scalaz.{Monad, MonadError, MonadPlus}

object Implicits {
  //TODO: proper variance
  object MonadErrorInstances {
    implicit def streamMonadPlus[F[+_]: MonadError[?[_], Throwable] : ZeroError[Throwable, ?[_]]]: MonadPlus[AsyncStream[F, ?]] = new ASMonadPlusForMonadError[F]
    implicit def impl[F[+_]: MonadError[?[_], Throwable] : ZeroError[Throwable, ?[_]]]: ASImpl[F] = new ASImplForMonadError[F]
  }

  def asStateTOps[F[+_]: Monad](implicit methods: ASImpl[F]) = new ASStateTOps[F]

  object ScalaFuture {
    implicit def scalaFutureZero(implicit ec: ExecutionContext): ZeroError[Throwable, Future] = new FutureZeroError()
  }
}
