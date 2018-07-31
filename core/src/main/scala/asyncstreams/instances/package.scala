package asyncstreams

import alleycats.EmptyK
import cats._
import cats.data.StateT
import cats.mtl.FunctorEmpty
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import EmptyKOrElse.ops._

import scala.language.higherKinds

package object instances {
  implicit def streamInstance[F[_]: Monad: EmptyKOrElse]: Monad[AsyncStream[F, ?]] with Alternative[AsyncStream[F, ?]] =
    new Monad[AsyncStream[F, ?]] with Alternative[AsyncStream[F, ?]] with StackSafeMonad[AsyncStream[F, ?]] {
      override def empty[A]: AsyncStream[F, A] = AsyncStream.empty

      override def combineK[A](x: AsyncStream[F, A], y: AsyncStream[F, A]): AsyncStream[F, A] = AsyncStream {
        x.data.map(step => Step(step.value, combineK(step.rest, y))).orElse(y.data)
      }

      override def pure[A](x: A) = AsyncStream(Step(x, empty[A]).pure[F])

      override def flatMap[A, B](fa: AsyncStream[F, A])(f: A => AsyncStream[F, B]): AsyncStream[F, B] = AsyncStream {
        fa.data.flatMap(step => f(step.value).data.map(step2 => Step(step2.value, combineK(step2.rest, flatMap(step.rest)(f)))))
      }
    }

  implicit def stateTFunctorEmpty[F[_], S](implicit fst: Functor[StateT[F, S, ?]], mf: Monad[F], emptyk: EmptyK[F]): FunctorEmpty[StateT[F, S, ?]] =
    new FunctorEmpty[StateT[F, S, ?]] {
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
}
