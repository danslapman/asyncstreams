package asyncstreams.stdFuture

import asyncstreams.Utils._
import asyncstreams.{AsyncStream, Implicits}
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaz.StateT
import scalaz.std.scalaFuture._
import scalaz.syntax.monadPlus._

class AsyncStreamMonadSyntaxTests extends FunSuite with Matchers {
  import Implicits.MonadErrorInstances._
  import Implicits.asStateTOps
  private val ftInstance = asStateTOps[Future]
  import ftInstance._

  private def wait[T](f: Future[T], d: Duration = 5.seconds): T = Await.result(f, d)

  test("foreach") {
    val fsm = StateT.stateTMonadState[Int, Future]

    val fstate = for {
      _ <- foreach(List(0, 1, 2).toAS[Future]) {
        v => fsm.modify(_ + 1)
      }
      v2 <- fsm.get
    } yield v2

    wait(fstate(0)) shouldBe (3, 3)
  }

  test("get, isEmpty") {
    case class State(counter: Int, stream: AsyncStream[Future, Int])
    val fsm = StateT.stateTMonadState[State, Future]
    implicit val fsmp = StateT.stateTMonadPlus[Int, Future](monadErrorFilter[Future])

    val stream = List(0, 1, 2, 3).toAS[Future]

    val fstate = for {
      _ <- fsm.whileM_(notEmpty(_.stream), for {
        s <- fsm.get
        newSV <- get[Int, State](s.stream)
        (newStream, el) = newSV
        _ <- fsm.put(State(s.counter + el, newStream))
      } yield ())
      v <- fsm.get
    } yield v.counter

    wait(fstate(State(0, stream)))._2 shouldBe 6
  }

  test("FState as generator") {
    val fsm = StateT.stateTMonadState[Int, Future]

    val stream = genS(0) {
      for {
        s <- fsm.get
        _ <- fsm.put(s + 1)
      } yield s
    } take 3

    wait(stream.to[List]) shouldBe (0 :: 1 :: 2 :: Nil)
  }

  test("Generate finite stream") {
    val fsm = StateT.stateTMonadState[Int, Future]
    implicit val fsmp = StateT.stateTMonadPlus[Int, Future](monadErrorFilter[Future])

    val stream = genS(0) {
      for {
        s <- fsm.get
        if s < 3
        _ <- fsm.put(s + 1)
      } yield s
    }

    wait(stream.to[List]) shouldBe (0 :: 1 :: 2 :: Nil)
  }
}
