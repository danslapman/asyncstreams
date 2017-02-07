package asyncstreams

import scala.annotation.unchecked.{uncheckedVariance => uV}
import scala.collection.GenIterable
import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds
import scalaz.Monad
import scalaz.syntax.monad._

case class AsyncStream[F[+_]: Monad, A](data: F[Step[A, AsyncStream[F, A]]]) {
  import AsyncStream._

  def foldLeft[B](start: B)(f: (B, A) => B): F[B] = {
    def impl(d: F[Step[A, AsyncStream[F, A]]], acc: F[B]): F[B] =
      d.flatMap {
        case END => acc
        case step => impl(step.rest.data, acc map (b => f(b, step.value)))
      }

    impl(data, start.point[F])
  }

  def to[Col[_]](implicit cbf: CanBuildFrom[Nothing, A, Col[A @uV]]): F[Col[A]] =
    foldLeft(cbf())((col, el) => col += el).map(_.result())


  def takeWhile(p: A => Boolean): AsyncStream[F, A] =
    new AsyncStream[F, A](data map {
      case END => END
      case step if !p(step.value) => END
      case step => Step(step.value, step.rest.takeWhile(p))
    })


  def take(n: Int): AsyncStream[F, A] =
    if (n <= 0) nil
    else AsyncStream(data.map {
      case END => END
      case p => Step(p.value, p.rest.take(n - 1))
    })

  def foreach[U](f: (A) => U): F[Unit] =
    foldLeft(())((_: Unit, a: A) => {f(a); ()})

  def foreachF[U](f: (A) => F[U]): F[Unit] =
    foldLeft(().point[F])((fu: F[Unit], a: A) => fu.flatMap(_ => f(a)).map(_ => ())).flatMap(identity)

  def flatten[B](implicit asIterable: A => GenIterable[B]): AsyncStream[F, B] = {
    val streamChunk = (p: Step[A, AsyncStream[F, A]]) =>
      concat(generate(asIterable(p.value))(it => if (it.nonEmpty) (it.head, it.tail).point[F] else ENDF[F]), p.rest.flatten)

    AsyncStream(data.flatMap {
      case END => ENDF[F]
      case step => streamChunk(step).data
    })
  }
}


object AsyncStream {
  def nil[F[+_]: Monad, A]: AsyncStream[F, A] = AsyncStream(ENDF[F])
  def single[F[+_]: Monad, A](item: A): AsyncStream[F, A] =
    AsyncStream(Step(item, nil[F, A]).point[F])

  def generate[F[+_]: Monad, S, A](start: S)(gen: S => F[(A, S)]): AsyncStream[F, A] =
    AsyncStream(gen(start).map {
      case END => END
      case (el, rest) => Step(el, generate(rest)(gen))
    })

  def concat[F[+_]: Monad, A](s1: AsyncStream[F, A], s2: AsyncStream[F, A]): AsyncStream[F, A] =
    new AsyncStream[F, A](s1.data.flatMap {
      case END => s2.data
      case step => Step(step.value, concat(step.rest, s2)).point[F]
    })
}

