package asyncstreams.scalaFutures

import asyncstreams.{streamOps, fStateOps}
import asyncstreams.{BaseSuite, fStateInstance}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaz.std.scalaFuture._

class FStateTests extends BaseSuite {
  private val so = streamOps[Future]
  import so._
  private val fso = fStateOps[Future]
  import fso._

  private def wait[T](f: Future[T]): T = Await.result(f, 10.seconds)

  test("FState in for-comprehensions") {
    val fsm = fStateInstance[Future, Int]

    val t = for {
      a <- fsm.point(10)
      b = a + 1
    } yield b

    wait(t(0)) shouldBe (11, 0)
  }

  test("gets & puts") {
    val fsm = fStateInstance[Future, Int]

    val t = for {
      _ <- fsm.whileM_(getS[Int] map (_ < 10), for {
        i <- getS[Int]
        _ <- putS(i + 1)
      } yield ())
      v1 <- getS[Int]
    } yield v1

    wait(t(0)) shouldBe (10, 10)
  }

  test("conds & mods") {
    implicit val fsm = fStateInstance[Future, Int]

    val t = for {
      _ <- fsm.whileM_(condS(_ < 10), modS(_ + 1))
      v1 <- getS[Int]
    } yield v1

    wait(t(0)) shouldBe (10, 10)
  }

  test("forM_") {
    val fsm = fStateInstance[Future, Int]

    val t = for {
      _ <- fsm.forM_(_ < 10, _ + 1) {
        fsm.point("AAAAAA")
      }
      v1 <- getS[Int]
    } yield v1

    wait(t(0)) shouldBe (10, 10)
  }
}
