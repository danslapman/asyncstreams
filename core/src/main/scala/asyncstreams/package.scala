import cats.{Monad, MonadError, FunctorFilter, Eval}
import cats.syntax.functorFilter._
import cats.syntax.applicative._

import scala.concurrent.Future
import scala.language.higherKinds

package object asyncstreams {
  type Step[A, B] = (A, Eval[B])

  implicit class AsyncStreamOps[F[_]: Monad: EmptyKOrElse, A](stream: AsyncStream[F, A]) {
    def ~::(el: A) = AsyncStream((el -> Eval.now(stream)).pure[F])
  }

  implicit class IterableToAS[T](it: Iterable[T]) {
    def toAS[F[_]: Monad: EmptyKOrElse]: AsyncStream[F, T] = AsyncStream.fromIterable(it)
  }

  implicit class FunctorEmptyWithFilter[F[_] : FunctorFilter, A](fa: F[A]) {
    def withFilter(f: A => Boolean): F[A] = fa.filter(f)
  }

  implicit def futureEmptyKOrElse(implicit me: MonadError[Future, Throwable]): EmptyKOrElse[Future] = new EmptyKOrElse[Future] {
    override def empty[A]: Future[A] = me.raiseError(new NoSuchElementException)

    override def orElse[A](fa: Future[A], default: => Future[A]): Future[A] =
      me.recoverWith(fa) { case _: NoSuchElementException  => default }
  }

  implicit val optionEmptyKOrElse: EmptyKOrElse[Option] = new EmptyKOrElse[Option] {
    override def empty[A]: Option[A] = None

    override def orElse[A](fa: Option[A], default: => Option[A]): Option[A] =
      fa.orElse(default)
  }
}
