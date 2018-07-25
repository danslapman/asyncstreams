import alleycats.EmptyK
import cats.{Alternative, Monad, MonadError}
import cats.mtl.FunctorEmpty
import cats.mtl.syntax.empty._
import cats.syntax.applicative._
import cats.syntax.applicativeError._

import scala.concurrent.Future
import scala.language.higherKinds

package object asyncstreams {
  implicit class AsyncStreamOps[F[_]: Monad, A](stream: => AsyncStream[F, A]) {
    def ~::(el: A) = AsyncStream(Step(el, stream).pure[F])
  }

  implicit class IterableToAS[T](it: Iterable[T]) {
    def toAS[F[_]: Monad](implicit methods: ASImpl[F]): AsyncStream[F, T] = methods.fromIterable(it)
  }

  implicit class FunctorEmptyWithFilter[F[_] : FunctorEmpty, A](fa: F[A]) {
    def withFilter(f: A => Boolean): F[A] = fa.filter(f)
  }

  implicit def streamInstance[F[_]: MonadError[?[_], Throwable]]: Monad[AsyncStream[F, ?]] with Alternative[AsyncStream[F, ?]] =
    new ASInstanceForMonadError[F]
  implicit def asimpl[F[_]: MonadError[?[_], Throwable]]: ASImpl[F] = new ASImplForMonadError[F]
  implicit def zeroK[F[_]](implicit me: MonadError[F, Throwable]): EmptyK[F] = new EmptyK[F] {
    override def empty[A]: F[A] = me.raiseError(new NoSuchElementException)
  }

  implicit def futureEmptyKOrElse(implicit me: MonadError[Future, NoSuchElementException]): EmptyKOrElse[Future] = new EmptyKOrElse[Future] {
    override def empty[A]: Future[A] = me.raiseError(new NoSuchElementException)

    override def orElse[A](fa: Future[A], default: => Future[A]): Future[A] = fa.handleErrorWith(_ => default)
  }
}
