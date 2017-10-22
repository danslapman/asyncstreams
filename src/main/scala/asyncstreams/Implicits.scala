package asyncstreams

import asyncstreams.typeclass.ZeroK

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scalaz.{Monad, MonadError, MonadPlus}

object Implicits {
  object MonadErrorInstances {
    implicit def streamMonadPlus[F[+_]: λ[`x[+_]` => MonadError[x, Throwable]] : ZeroK]: MonadPlus[AsyncStream[F, ?]] = new ASMonadPlusForMonadError[F]
    implicit def impl[F[+_]: λ[`x[+_]` => MonadError[x, Throwable]] : ZeroK]: ASImpl[F] = new ASImplForMonadError[F]
  }

  def asStateTOps[F[+_]: Monad](implicit methods: ASImpl[F]) = new ASStateTOps[F]

  object ScalaFuture {
    implicit def scalaFutureZero(implicit ec: ExecutionContext): ZeroK[Future] = new ZeroK[Future] {
      override def zero[A]: Future[A] = Future.failed(new NoSuchElementException)
    }
  }
}
