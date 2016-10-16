package asyncstreams

import scala.concurrent.{ExecutionContext, Future}
import scalaz.std.scalaFuture._
import scalaz.syntax.std.option._
import scalaz.syntax.monad._
import scalaz.OptionT.{optionT => opT}

case class AsyncStream[A](data: Future[Chunk[A, AsyncStream[A]]]) {
  import AsyncStream._

  def foldLeft[B](start: B)(f: (B, A) => B)(implicit executor: ExecutionContext): Future[B] = {
    def impl(d: Future[Chunk[A, AsyncStream[A]]], acc: Future[B]): Future[B] =
      d.flatMap(chunk => chunk.map(p => impl(p.second.data, acc map (b => f(b, p.first)))).getOrElse(acc))

    impl(data, Future(start))
  }

  def toList(implicit executor: ExecutionContext): Future[List[A]] =
    foldLeft[List[A]](Nil)((list, el) => el :: list) map (_.reverse)

  def takeWhile(p: A => Boolean)(implicit executor: ExecutionContext): AsyncStream[A] =
    new AsyncStream[A](data map {
      case None => None
      case Some(pair) if !p(pair.first) => None
      case Some(pair) => Some(Pair(pair.first, pair.second.takeWhile(p)))
    })

  def take(n: Int)(implicit executor: ExecutionContext): AsyncStream[A] =
    if (n <= 0) nil
    else AsyncStream(opT(data).map(p => Pair(p.first, p.second.take(n - 1))).run)
}

object AsyncStream {
  def nil[A](implicit executor: ExecutionContext): AsyncStream[A] = AsyncStream(None.point[Future])
  def single[A](item: A)(implicit executor: ExecutionContext): AsyncStream[A] =
    AsyncStream(Pair(item, nil[A]).some.point[Future])

  def generate[S, A](start: S)(gen: S => Future[Option[(S, A)]])(implicit executor: ExecutionContext): AsyncStream[A] =
    AsyncStream(opT(gen(start)).map(p => Pair(p._2, generate(p._1)(gen))).run)

  def concat[A](s1: AsyncStream[A], s2: AsyncStream[A])(implicit executor: ExecutionContext): AsyncStream[A] =
    new AsyncStream[A](s1.data.flatMap {
      case None => s2.data
      case Some(p) => Pair(p.first, concat(p.second, s2)).some.point[Future]
    })
}
