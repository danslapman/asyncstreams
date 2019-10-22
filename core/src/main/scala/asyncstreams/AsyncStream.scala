package asyncstreams

import cats.kernel.Monoid
import cats.{Applicative, Eval, Monad, MonoidK, ~>}
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import cats.syntax.semigroup._
import cats.syntax.semigroupk._
import alleycats.Pure
import cats.data.{State, StateT}

import scala.language.higherKinds

class AsyncStream[F[_]: Monad: MonoidK, A](private[asyncstreams] val data: F[Step[A, AsyncStream[F, A]]]) {
  private val MK = MonoidK[F]

  def foldLeft[B](init: B)(f: (B, A) => B): F[B] = {
    def impl(d: F[Step[A, AsyncStream[F, A]]], acc: F[B]): F[B] =
      d.flatMap(step => impl(step._2.value.data, acc.map(b => f(b, step._1)))) <+> acc

    impl(data, init.pure[F])
  }

  def ++(other: AsyncStream[F, A]): AsyncStream[F, A] =
    AsyncStream.concat(this, other)

  def takeWhile(p: A => Boolean): AsyncStream[F, A] = AsyncStream {
    data.flatMap {
      case step if !p(step._1) => MK.empty
      case step => (step._1 -> step._2.map(_.takeWhile(p))).pure[F]
    }
  }

  def take(n: Int): AsyncStream[F, A] =
    if (n <= 0) AsyncStream.empty
    else AsyncStream {
      data.map(p => p._1 -> p._2.map(_.take(n - 1)))
    }

  def drop(n: Int): AsyncStream[F, A] =
    if (n <= 0) this
    else AsyncStream {
      data.flatMap(p => p._2.value.drop(n - 1).data)
    }

  def foreach[U](f: A => U): F[Unit] =
    data.flatMap { case (value, rest) =>
      f(value).pure[F] >> rest.value.foreach(f)
    } <+> Applicative[F].unit

  def foreachF[U](f: A => F[U]): F[Unit] =
    data.flatMap { case (value, rest) =>
      f(value) >> rest.value.foreachF(f)
    } <+> Applicative[F].unit

  def flatten[B](implicit asIterable: A => Iterable[B]): AsyncStream[F, B] = {
    def streamChunk(step: Step[A, AsyncStream[F, A]]): AsyncStream[F, B] =
      AsyncStream.concat(
        AsyncStream.fromIterable(asIterable(step._1)),
        step._2.value.flatten
      )

    AsyncStream(data.flatMap(step => streamChunk(step).data))
  }

  def isEmpty: F[Boolean] = data.map(_ => false) <+> true.pure[F]
  def nonEmpty: F[Boolean] = data.map(_ => true) <+> false.pure[F]

  def map[B](f: A => B): AsyncStream[F, B] = AsyncStream {
    data.map(s => f(s._1) -> s._2.map(_.map(f)))
  }

  def mapF[B](f: A => F[B]): AsyncStream[F, B] = AsyncStream {
    data.flatMap(s => f(s._1).map(nv => nv -> s._2.map(_.mapF(f))))
  }

  def mapK[G[_]: Monad: MonoidK](fk: F ~> G): AsyncStream[G, A] = AsyncStream {
    fk.apply(data.map { case (a, eval) => a -> eval.map(_.mapK(fk))})
  }

  def flatMap[B](f: A => AsyncStream[F, B]): AsyncStream[F, B] = AsyncStream {
    for {
      (head, tail) <- data
      headStream = f(head)
      (newHead, newTail) <- headStream.data
    } yield newHead -> newTail.flatMap(ta => tail.map(tb => AsyncStream.concat(ta, tb.flatMap(f))))
  }

  def filter(p: A => Boolean): AsyncStream[F, A] = AsyncStream {
    data.flatMap { s =>
      if (p(s._1)) (s._1 -> s._2.map(_.filter(p))).pure[F]
      else s._2.value.filter(p).data
    }
  }

  def withFilter(p: A => Boolean): AsyncStream[F, A] = filter(p)

  def find(p: A => Boolean): F[Option[A]] = {
    data.flatMap { s =>
      if (p(s._1)) s._1.some.pure[F]
      else s._2.value.find(p)
    } <+> none[A].pure[F]
  }

  def findF(p: A => F[Boolean]): F[Option[A]] = {
    data.flatMap { s =>
      p(s._1).flatMap {
        case true => s._1.some.pure[F]
        case false => s._2.value.findF(p)
      }
    } <+> none[A].pure[F]
  }

  def partition(p: A => Boolean): (AsyncStream[F, A], AsyncStream[F, A]) = (filter(p), filter(p.andThen(!_)))

  def foldMap[B: Monoid](f: A => B): F[B] = {
    foldLeft(Monoid[B].empty)((b, a) => b |+| f(a))
  }

  def zip[B](sb: AsyncStream[F, B]): AsyncStream[F, (A, B)] = AsyncStream {
    for {
      stepA <- data
      stepB <- sb.data
    } yield (stepA._1, stepB._1) -> stepA._2.flatMap(ra => stepB._2.map(rb => ra zip rb))
  }

  def zipWithIndex: AsyncStream[F, (A, Int)] =
    zip(AsyncStream.unfold(0)(_ + 1))
}

object AsyncStream {
  private[asyncstreams] def apply[F[_]: Monad: MonoidK, A](data: => F[Step[A, AsyncStream[F, A]]]): AsyncStream[F, A] =
    new AsyncStream(data)

  private[asyncstreams] def evalF[F[_]: Pure]: Eval ~> F = λ[Eval ~> F](e => Pure[F].pure(e.value))

  def empty[F[_]: Monad: MonoidK, A]: AsyncStream[F, A] = AsyncStream(MonoidK[F].empty)

  def fromIterable[F[_]: Monad: MonoidK, T](it: Iterable[T]): AsyncStream[F, T] = AsyncStream {
    if (it.nonEmpty) (it.head -> Eval.later(fromIterable(it.tail))).pure[F] else MonoidK[F].empty
  }

  def unfoldS[F[_]: Monad: MonoidK, S, T](initial: S)(gen: State[S, T]): AsyncStream[F, T] =
    unfoldST(initial)(gen.mapK(evalF))

  def unfoldST[F[_]: Monad: MonoidK, S, T](initial: S)(gen: StateT[F, S, T]): AsyncStream[F, T] = AsyncStream {
    gen.run(initial).map {
      case (s, t) => t -> Eval.later(unfoldST(s)(gen))
    }
  }

  def unfold[F[_]: Monad: MonoidK, T](start: T)(makeNext: T => T): AsyncStream[F, T] =
    unfoldS(start)(StateT(s => Eval.later((makeNext(s), s))))

  def unfoldM[F[_]: Monad: MonoidK, T](start: T)(makeNext: T => F[T]): AsyncStream[F, T] =
    unfoldST(start)(StateT(s => makeNext(s).map(n => (n, s))))

  def unfoldMM[F[_]: Monad: MonoidK, T](start: F[T])(makeNext: T => F[T]): AsyncStream[F, T] = AsyncStream {
    start.flatMap(unfoldST(_)(StateT(s => makeNext(s).map(n => (n, s)))).data)
  }

  def continually[F[_]: Monad: MonoidK, T](elem: => T): AsyncStream[F, T] = AsyncStream {
    (elem -> Eval.later(AsyncStream.continually(elem))).pure[F]
  }

  def continuallyF[F[_]: Monad: MonoidK, T](elem: => F[T]): AsyncStream[F, T] = AsyncStream {
    elem.map(_ -> Eval.later(continuallyF(elem)))
  }

  def continuallyEval[F[_]: Monad: MonoidK, T](elem: Eval[T]): AsyncStream[F, T] = AsyncStream {
    (elem.value -> Eval.later(AsyncStream.continuallyEval(elem))).pure[F]
  }

  def concat[F[_]: Monad: MonoidK, A](x: AsyncStream[F, A], y: AsyncStream[F, A]): AsyncStream[F, A] = AsyncStream {
    x.data.map(step => step._1 -> step._2.map(concat(_, y))) <+> y.data
  }
}