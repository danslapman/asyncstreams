package asyncstreams.stdFuture

import asyncstreams.Implicits
import asyncstreams.Utils._
import cats.instances.future._
import cats.mtl.instances.state._
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class AsyncStreamMonadSyntaxTests extends FunSuite with Matchers {
  import Implicits.MonadErrorInstances._
  import Implicits.asStateTOps
  private val ftInstance = asStateTOps[Future]
  import ftInstance._

  private def wait[T](f: Future[T], d: Duration = 5.seconds): T = Await.result(f, d)

  test("foreach") {
    val ms = stateState[Future, Int]

    val fstate = for {
      _ <- foreach(List(0, 1, 2, 3).toAS[Future]) {
        v => ms.modify(_ + v)
      }
      v2 <- ms.get
    } yield v2

    wait(fstate.run(0)) shouldBe (6, 6)
  }

  /*
  test("get, isEmpty") {
    case class StateCase(counter: Int, stream: AsyncStream[Future, Int])
    val ms = stateState[Future, StateCase]

    val stream = List(0, 1, 2, 3).toAS[Future]

    val fstate = for {
      _ <- ms.monad.whileM_(notEmpty(cs.stream))(for {
        s <- ms.get
        newSV <- get[Int, StateCase](s.stream)
        (newStream, el) = newSV
        _ <- ms.set(StateCase(s.counter + el, newStream))
      } yield ())
      v <- ms.get
    } yield v.counter

    wait(fstate.run(StateCase(0, stream)))._2 shouldBe 6
  }
  */

  test("FState as generator") {
    val ms = stateState[Future, Int]

    val stream = genS(0) {
      for {
        s <- ms.get
        _ <- ms.set(s + 1)
      } yield s
    } take 3

    wait(stream.to[List]) shouldBe (0 :: 1 :: 2 :: Nil)
  }

  /*
  test("Generate finite stream") {
    val ms = stateState[Future, Int]

    val stream = genS(0) {
      for {
        s <- ms.get
        if s < 3
        _ <- ms.set(s + 1)
      } yield s
    }

    wait(stream.to[List]) shouldBe (0 :: 1 :: 2 :: Nil)
  }
  */
}
