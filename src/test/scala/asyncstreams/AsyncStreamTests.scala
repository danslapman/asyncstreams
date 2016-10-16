package asyncstreams

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import asyncstreams.AsyncStream._

import scalaz.std.scalaFuture._
import scalaz.syntax.std.option._
import scalaz.syntax.monad._
import scalaz.syntax.std.boolean._


class AsyncStreamTests extends BaseSuite {
  private def makeStream(l: List[Int]) = generate(l)(l => l.nonEmpty.option((l.tail, l.head)).point[Future])

  private def makeInfStream = generate(0)(v => Future((v + 1, v).some))

  private def wait[T](f: Future[T]): T = Await.result(f, 10.seconds)

  test("foldLeft") {
    val s2 = makeStream(2 :: 3 :: Nil)
    val f = s2.foldLeft(List[Int]())((list, el) => el :: list)
    wait(f) shouldBe List(3, 2)
  }

  test("concat") {
    val s1 = makeStream(0 :: 1 :: Nil)
    val s2 = makeStream(2 :: 3 :: Nil)
    val f = concat(s1, s2)
    wait(f.toList) shouldBe List(0, 1, 2, 3)
  }

  test("working as monad") {
    val s1 = makeStream(0 :: 1 :: Nil)
    val s2 = makeStream(2 :: 3 :: Nil)

    val res = for {
      v1 <- s1
      v2 <- s2
    } yield v1 * v2

    wait(res.toList) shouldBe List(0, 0, 2, 3)
  }

  test("takeWhile") {
    val r = makeInfStream.takeWhile(_ < 4)
    wait(r.toList) shouldBe List(0, 1, 2, 3)
  }

  test("take") {
    val r = makeInfStream.take(3)
    wait(r.toList) shouldBe List(0, 1, 2)
  }

  test("folding large stream should not crash") {
    val r = makeInfStream.takeWhile(_ < 1000000)
    wait(r.toList) shouldBe (0 to 999999)
  }
}
