package asyncstreams

import alleycats.EmptyK
import cats._
import cats.data.StateT
import cats.FunctorFilter
import cats.syntax.applicative._

import scala.language.higherKinds

package object instances {
  implicit def stateTFunctorEmpty[F[_], S](implicit fst: Functor[StateT[F, S, ?]], mf: Monad[F], emptyk: EmptyK[F]): FunctorFilter[StateT[F, S, ?]] =
    new FunctorFilter[StateT[F, S, ?]] {
      override def mapFilter[A, B](fa: StateT[F, S, A])(f: A => Option[B]): StateT[F, S, B] =
        collect(fa.map(f)) { case Some(e) => e }

      override def collect[A, B](fa: StateT[F, S, A])(f: PartialFunction[A, B]): StateT[F, S, B] =
        fa.flatMapF(a => if (f.isDefinedAt(a)) f(a).pure[F] else emptyk.empty)

      override def flattenOption[A](fa: StateT[F, S, Option[A]]): StateT[F, S, A] =
        collect(fa) { case Some(e) => e }

      override def filter[A](fa: StateT[F, S, A])(f: A => Boolean): StateT[F, S, A] =
        fa.flatMapF(a => if(f(a)) a.pure[F] else emptyk.empty)

      override val functor: Functor[StateT[F, S, ?]] = fst
    }

  implicit def asyncStreamInstance[F[_]: Monad: MonoidK]: Monad[AsyncStream[F, ?]] with Alternative[AsyncStream[F, ?]] =
    new Monad[AsyncStream[F, ?]] with Alternative[AsyncStream[F, ?]] with StackSafeMonad[AsyncStream[F, ?]] {
      override def pure[A](x: A): AsyncStream[F, A] = x ~:: ANil[F, A]

      override def map[A, B](fa: AsyncStream[F, A])(f: A => B): AsyncStream[F, B] =
        fa.map(f)

      override def flatMap[A, B](fa: AsyncStream[F, A])(f: A => AsyncStream[F, B]): AsyncStream[F, B] =
        fa.flatMap(f)

      override def empty[A]: AsyncStream[F, A] = ANil[F, A]

      override def combineK[A](x: AsyncStream[F, A], y: AsyncStream[F, A]): AsyncStream[F, A] =
        AsyncStream.concat(x, y)
    }
}
