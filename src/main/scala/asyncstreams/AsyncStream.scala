package asyncstreams

import scala.annotation.unchecked.{uncheckedVariance => uV}
import scala.collection.GenIterable
import scala.collection.generic.CanBuildFrom
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

case class AsyncStream[A](data: Future[Step[A, AsyncStream[A]]]) {
  import AsyncStream._

  def foldLeft[B](start: B)(f: (B, A) => B)(implicit executor: ExecutionContext): Future[B] = {
    def impl(d: Future[Step[A, AsyncStream[A]]], acc: Future[B]): Future[B] =
      d.flatMap {
        case END => acc
        case step => impl(step.rest.data, acc map (b => f(b, step.value)))
      }

    impl(data, Future(start))
  }

  def to[Col[_]](implicit executor: ExecutionContext, cbf: CanBuildFrom[Nothing, A, Col[A @uV]]): Future[Col[A]] =
    foldLeft(cbf())((col, el) => col += el).map(_.result())


  def takeWhile(p: A => Boolean)(implicit executor: ExecutionContext): AsyncStream[A] =
    new AsyncStream[A](data map {
      case END => END
      case step if !p(step.value) => END
      case step => Step(step.value, step.rest.takeWhile(p))
    })


  def take(n: Int)(implicit executor: ExecutionContext): AsyncStream[A] =
    if (n <= 0) nil
    else AsyncStream(data.map {
      case END => END
      case p => Step(p.value, p.rest.take(n - 1))
    })

  def foreach[U](f: (A) => U)(implicit executor: ExecutionContext): Future[Unit] =
    foldLeft(())((_: Unit, a: A) => {f(a); ()})

  def foreachF[U](f: (A) => Future[U])(implicit executor: ExecutionContext): Future[Unit] =
    foldLeft(Future(()))((fu: Future[Unit], a: A) => fu.flatMap(_ => f(a)).map(_ => ())).flatMap(identity)

  def flatten[B](implicit asIterable: A => GenIterable[B], executor: ExecutionContext): AsyncStream[B] = {
    val streamChunk = (p: Step[A, AsyncStream[A]]) =>
      concat(generate(asIterable(p.value))(it => if (it.nonEmpty) Future(it.head, it.tail) else ENDF), p.rest.flatten)

    AsyncStream(data.flatMap {
      case END => ENDF
      case step => streamChunk(step).data
    })
  }
}


object AsyncStream {
  def nil[A](implicit executor: ExecutionContext): AsyncStream[A] = AsyncStream(ENDF)
  def single[A](item: A)(implicit executor: ExecutionContext): AsyncStream[A] =
    AsyncStream(Future(Step(item, nil[A])))

  def generate[S, A](start: S)(gen: S => Future[(A, S)])(implicit executor: ExecutionContext): AsyncStream[A] =
    AsyncStream(gen(start).map {
      case END => END
      case (el, rest) => Step(el, generate(rest)(gen))
    })

  def concat[A](s1: AsyncStream[A], s2: AsyncStream[A])(implicit executor: ExecutionContext): AsyncStream[A] =
    new AsyncStream[A](s1.data.flatMap {
      case END => s2.data
      case step => Future(Step(step.value, concat(step.rest, s2)))
    })
}

