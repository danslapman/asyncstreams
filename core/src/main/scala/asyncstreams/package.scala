import cats.{Monad, MonadError}
import cats.mtl.FunctorEmpty
import cats.mtl.syntax.empty._
import cats.syntax.applicative._

import scala.concurrent.Future
import scala.language.higherKinds

package object asyncstreams {
  implicit class AsyncStreamOps[F[_]: Monad: EmptyKOrElse, A](stream: => AsyncStream[F, A]) {
    def ~::(el: A) = AsyncStream(Step(el, stream).pure[F])
  }

  implicit class IterableToAS[T](it: Iterable[T]) {
    def toAS[F[_]: Monad: EmptyKOrElse]: AsyncStream[F, T] = AsyncStream.fromIterable(it)
  }

  implicit class FunctorEmptyWithFilter[F[_] : FunctorEmpty, A](fa: F[A]) {
    def withFilter(f: A => Boolean): F[A] = fa.filter(f)
  }

  implicit def futureEmptyKOrElse(implicit me: MonadError[Future, Throwable]): EmptyKOrElse[Future] = new EmptyKOrElse[Future] {
    override def empty[A]: Future[A] = me.raiseError(new NoSuchElementException)

    override def orElse[A](fa: Future[A], default: => Future[A]): Future[A] =
      me.recoverWith(fa) { case _: NoSuchElementException  => default }
  }
}
