package asyncstreams.stdFuture

import java.util.concurrent.Executors

import asyncstreams._
import asyncstreams.instances._
import asyncstreams.ops.StateTOps
import cats.instances.future._
import cats.mtl.instances.state._
import cats.mtl.syntax.empty._
import org.scalatest.{AsyncFunSuite, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class AsyncStreamMonadSyntaxTests extends AsyncFunSuite with Matchers {
  override implicit def executionContext: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  private val ops = StateTOps[Future]
  import ops._

  test("foreach") {
    val ms = stateState[Future, Int]

    val fstate = for {
      _ <- foreach(List(0, 1, 2, 3).toAS[Future]) {
        v => ms.modify(_ + v)
      }
      v2 <- ms.get
    } yield v2

    fstate.run(0).map(_ shouldBe (6, 6))
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

    stream.to[List].map(_ shouldBe (0 :: 1 :: 2 :: Nil))
  }

  test("Generate finite stream") {
    val ms = stateState[Future, Int]

    val stream = genS(0) {
      for {
        s <- ms.get
        if s < 3
        _ <- ms.set(s + 1)
      } yield s
    }

    stream.to[List].map(_ shouldBe (0 :: 1 :: 2 :: Nil))
  }
}
