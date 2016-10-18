package asyncstreams

import scala.annotation.unchecked.{uncheckedVariance => uV}
import scala.collection.generic.CanBuildFrom
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scalaz.std.scalaFuture._
import scalaz.syntax.monad._

case class AsyncStream[A](data: Future[Pair[A, AsyncStream[A]]]) {
  import AsyncStream._

  def foldLeft[B](start: B)(f: (B, A) => B)(implicit executor: ExecutionContext): Future[B] = {
    def impl(d: Future[Pair[A, AsyncStream[A]]], acc: Future[B]): Future[B] =
      d.flatMap {
        case null => acc
        case pair => impl(pair.second.data, acc map (b => f(b, pair.first)))
      }

    impl(data, Future(start))
  }

  def to[Col[_]](implicit executor: ExecutionContext, cbf: CanBuildFrom[Nothing, A, Col[A @uV]]): Future[Col[A]] =
    foldLeft(cbf())((col, el) => col += el).map(_.result())


  def takeWhile(p: A => Boolean)(implicit executor: ExecutionContext): AsyncStream[A] =
    new AsyncStream[A](data map {
      case null => END
      case pair if !p(pair.first) => END
      case pair => Pair(pair.first, pair.second.takeWhile(p))
    })


  def take(n: Int)(implicit executor: ExecutionContext): AsyncStream[A] =
    if (n <= 0) nil
    else AsyncStream(data.map {
      case null => END
      case p => Pair(p.first, p.second.take(n - 1))
    })
}


object AsyncStream {
  def nil[A](implicit executor: ExecutionContext): AsyncStream[A] = AsyncStream(ENDF)
  def single[A](item: A)(implicit executor: ExecutionContext): AsyncStream[A] =
    AsyncStream(Pair(item, nil[A]).point[Future])

  def generate[S, A](start: S)(gen: S => Future[(A, S)])(implicit executor: ExecutionContext): AsyncStream[A] =
    AsyncStream(gen(start).map {
      case null => END
      case (el, rest) => Pair(el, generate(rest)(gen))
    })

  def concat[A](s1: AsyncStream[A], s2: AsyncStream[A])(implicit executor: ExecutionContext): AsyncStream[A] =
    new AsyncStream[A](s1.data.flatMap {
      case null => s2.data
      case p => Pair(p.first, concat(p.second, s2)).point[Future]
    })
}

