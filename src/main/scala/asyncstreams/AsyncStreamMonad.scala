package asyncstreams

import scala.concurrent.{ExecutionContext, Future}
import scalaz.std.scalaFuture._
import scalaz.syntax.std.option._
import scalaz.syntax.monad._
import scalaz.MonadPlus
import scalaz.OptionT.{optionT => opT}

class AsyncStreamMonad(implicit executor: ExecutionContext) extends MonadPlus[AsyncStream] {
  import AsyncStream._

  override def empty[A] = nil[A]

  override def point[A](a: => A): AsyncStream[A] = single(a)

  override def plus[A](a: AsyncStream[A], b: => AsyncStream[A]) = concat(a, b)

  override def bind[A, B](ma: AsyncStream[A])(f: A => AsyncStream[B]): AsyncStream[B] = {
    val resData = opT(ma.data).flatMap(pair =>
      opT(f(pair.first).data).map(pair2 =>
        Pair(pair2.first, concat(pair2.second, bind(pair.second)(f)))
      )
    ).run
    AsyncStream(resData)
  }
}
