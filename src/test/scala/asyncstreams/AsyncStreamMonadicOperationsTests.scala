package asyncstreams

import monadops._
import AsyncStream._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class AsyncStreamMonadicOperationsTests extends BaseSuite {
  def makeStream(l: List[Int]) = generate(l)(l => if (l.isEmpty) ENDF else Future((l.head, l.tail)))

  private def wait[T](f: Future[T]): T = Await.result(f, 10.seconds)

  test("foreach") {
    implicit val fsm = fStateInstance[Int]

    val fstate = for {
      _ <- foreach(makeStream(0 :: 1 :: 2 :: Nil)) {
        v => modS[Int](_ + 1)
      }
      v2 <- getS[Int]
    } yield v2

    wait(fstate(0)) shouldBe (3, 3)
  }

  test("get, isEmpty") {
    case class State(counter: Int, stream: AsyncStream[Int])
    implicit val fsm = fStateInstance[State]

    val stream = makeStream(0 :: 1 :: 2 :: 3 :: Nil)

    val fstate = for {
      _ <- fsm.whileM_(notEmpty(_.stream), for {
        s <- getS[State]
        (el, newStream) <- get[Int, State](s.stream)
        _ <- putS[State](State(s.counter + el, newStream))
      } yield ())
      v <- getS[State]
    } yield v.counter

    wait(fstate(State(0, stream)))._1 shouldBe 6
  }

  test("FState as generator") {
    implicit val fsm = fStateInstance[Int]

    val stream = generateS(0) {
      for {
        s <- getS[Int]
        _ <- putS[Int](s + 1)
      } yield s
    } take 3

    wait(stream.to[List]) shouldBe (0 :: 1 :: 2 :: Nil)
  }

  test("Generate finite stream") {
    implicit val fsm = fStateInstance[Int]

    val stream = generateS(0) {
      for {
        s <- getS[Int]
        if s < 3
        _ <- putS(s + 1)
      } yield s
    }

    wait(stream.to[List]) shouldBe (0 :: 1 :: 2 :: Nil)
  }
}
