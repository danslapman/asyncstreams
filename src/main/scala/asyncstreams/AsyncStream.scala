package asyncstreams

import scala.annotation.unchecked.{uncheckedVariance => uV}
import scala.collection.GenIterable
import scala.collection.generic.CanBuildFrom
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

case class AsyncStream[A](data: Future[Pair[A, AsyncStream[A]]]) {
  import AsyncStream._

  def foldLeft[B](start: B)(f: (B, A) => B)(implicit executor: ExecutionContext): Future[B] = {
    def impl(d: Future[Pair[A, AsyncStream[A]]], acc: Future[B]): Future[B] =
      d.flatMap {
        case END => acc
        case pair => impl(pair.second.data, acc map (b => f(b, pair.first)))
      }

    impl(data, Future(start))
  }

  def to[Col[_]](implicit executor: ExecutionContext, cbf: CanBuildFrom[Nothing, A, Col[A @uV]]): Future[Col[A]] =
    foldLeft(cbf())((col, el) => col += el).map(_.result())


  def takeWhile(p: A => Boolean)(implicit executor: ExecutionContext): AsyncStream[A] =
    new AsyncStream[A](data map {
      case END => END
      case pair if !p(pair.first) => END
      case pair => Pair(pair.first, pair.second.takeWhile(p))
    })


  def take(n: Int)(implicit executor: ExecutionContext): AsyncStream[A] =
    if (n <= 0) nil
    else AsyncStream(data.map {
      case END => END
      case p => Pair(p.first, p.second.take(n - 1))
    })

  def foreach[U](f: (A) => U)(implicit executor: ExecutionContext): Future[Unit] =
    foldLeft(())((_: Unit, a: A) => {f(a); ()})

  def foreachF[U](f: (A) => Future[U])(implicit executor: ExecutionContext): Future[Unit] =
    foldLeft(Future(()))((fu: Future[Unit], a: A) => fu.flatMap(_ => f(a)).map(_ => ())).flatMap(u => u)

  def flatten[B](implicit asIterable: A => GenIterable[B], executor: ExecutionContext): AsyncStream[B] = {
    val streamChunk = (p: Pair[A, AsyncStream[A]]) =>
      concat(generate(asIterable(p.first))(it => if (it.nonEmpty) Future(it.head, it.tail) else ENDF), p.second.flatten)

    AsyncStream(data.flatMap {
      case END => ENDF
      case pair => streamChunk(pair).data
    })
  }
}


object AsyncStream {
  def nil[A](implicit executor: ExecutionContext): AsyncStream[A] = AsyncStream(ENDF)
  def single[A](item: A)(implicit executor: ExecutionContext): AsyncStream[A] =
    AsyncStream(Future(Pair(item, nil[A])))

  def generate[S, A](start: S)(gen: S => Future[(A, S)])(implicit executor: ExecutionContext): AsyncStream[A] =
    AsyncStream(gen(start).map {
      case END => END
      case (el, rest) => Pair(el, generate(rest)(gen))
    })

  def concat[A](s1: AsyncStream[A], s2: AsyncStream[A])(implicit executor: ExecutionContext): AsyncStream[A] =
    new AsyncStream[A](s1.data.flatMap {
      case END => s2.data
      case p => Future(Pair(p.first, concat(p.second, s2)))
    })
}

