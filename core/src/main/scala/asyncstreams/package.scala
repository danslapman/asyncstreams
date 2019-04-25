import alleycats.EmptyK
import cats.{Eval, FunctorFilter, Monad, MonadError, MonoidK}
import cats.syntax.functorFilter._
import cats.syntax.applicative._

import scala.concurrent.Future
import scala.language.higherKinds

package object asyncstreams {
  type Step[A, B] = (A, Eval[B])

  implicit class AsyncStreamOps[F[_]: Monad: MonoidK, A](stream: AsyncStream[F, A]) {
    def ~::(el: A) = AsyncStream((el -> Eval.now(stream)).pure[F])
  }

  implicit class IterableToAS[T](it: Iterable[T]) {
    def toAS[F[_]: Monad: MonoidK]: AsyncStream[F, T] = AsyncStream.fromIterable(it)
  }

  implicit class FunctorEmptyWithFilter[F[_] : FunctorFilter, A](fa: F[A]) {
    def withFilter(f: A => Boolean): F[A] = fa.filter(f)
  }

  implicit def monoidKForFuture(implicit me: MonadError[Future, Throwable]): MonoidK[Future] = new MonoidK[Future] {
    override def empty[A]: Future[A] = me.raiseError(new NoSuchElementException)

    override def combineK[A](x: Future[A], y: Future[A]): Future[A] =
      me.recoverWith(x) { case _: NoSuchElementException => y }
  }

  implicit def emptyKForMonoidK[F[_]: MonoidK]: EmptyK[F] = new EmptyK[F] {
    override def empty[A]: F[A] = MonoidK[F].empty[A]
  }

  object ANil {
    def apply[F[_]: Monad: MonoidK, A]: AsyncStream[F, A] =
      AsyncStream.empty[F, A]
  }
}
