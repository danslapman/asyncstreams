package asyncstreams

import alleycats.EmptyK
import cats.data.StateT
import cats.mtl.FunctorEmpty
import cats.syntax.applicative._
import cats.{Functor, Monad}

import scala.language.higherKinds

class StateTFunctorEmpty[F[_], S](implicit val functor: Functor[StateT[F, S, ?]], mf: Monad[F], emptyk: EmptyK[F]) extends FunctorEmpty[StateT[F, S, ?]] {
  override def mapFilter[A, B](fa: StateT[F, S, A])(f: A => Option[B]): StateT[F, S, B] =
    collect(fa.map(f)) { case Some(e) => e }

  override def collect[A, B](fa: StateT[F, S, A])(f: PartialFunction[A, B]): StateT[F, S, B] =
    fa.flatMapF(a => if (f.isDefinedAt(a)) f(a).pure[F] else emptyk.empty)

  override def flattenOption[A](fa: StateT[F, S, Option[A]]): StateT[F, S, A] =
    collect(fa) { case Some(e) => e }

  override def filter[A](fa: StateT[F, S, A])(f: A => Boolean): StateT[F, S, A] =
    fa.flatMapF(a => if(f(a)) a.pure[F] else emptyk.empty)
}
