import alleycats.EmptyK
import cats.{Alternative, Monad, MonadError}
import cats.mtl.FunctorEmpty
import cats.mtl.syntax.empty._
import cats.syntax.applicative._

import scala.language.higherKinds

package object asyncstreams {
  implicit class AsyncStreamOps[F[+_]: Monad, A](stream: => AsyncStream[F, A]) {
    def ~::(el: A) = AsyncStream(Step(el, stream).pure[F])
  }

  implicit class IterableToAS[T](it: Iterable[T]) {
    def toAS[F[+_]: Monad](implicit methods: ASImpl[F]): AsyncStream[F, T] = methods.fromIterable(it)
  }

  implicit class FunctorWithFilter[F[_] : FunctorEmpty, A](fa: F[A]) {
    def withFilter(f: A => Boolean): F[A] = fa.filter(f)
  }

  implicit def streamInstance[F[+_]: λ[`x[+_]` => MonadError[x, Throwable]]]: Monad[AsyncStream[F, +?]] with Alternative[AsyncStream[F, +?]] = new ASInstanceForMonadError[F]
  implicit def asimpl[F[+_]: λ[`x[+_]` => MonadError[x, Throwable]]]: ASImpl[F] = new ASImplForMonadError[F]
  implicit def zeroK[F[+_]](implicit me: MonadError[F, Throwable]): EmptyK[F] = new EmptyK[F] {
    override def empty[A]: F[A] = me.raiseError(new NoSuchElementException)
  }
}
