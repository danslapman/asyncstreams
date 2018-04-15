package asyncstreams.catsIO

import asyncstreams._
import cats.effect.IO
import cats.effect.IO._
import cats.syntax.semigroupk._
import org.scalatest.{AsyncFunSuite, Matchers}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class AsyncStreamTestsWithCatsIO extends AsyncFunSuite with Matchers {
  private def makeInfStream = AsyncStream.unfold[IO, Int](0)(_ + 1)

  test("composition operator") {
    val s = 1 ~:: 2 ~:: 3 ~:: AsyncStream.asyncNil[IO, Int]
    s.to[List].unsafeToFuture().map(_ shouldBe List(1, 2, 3))
  }

  test("foldLeft") {
    val s2 = List(2, 3).toAS[IO]
    val f = implicitly[ASImpl[IO]].collectLeft(s2)(List[Int]())((list, el) => el :: list)
    f.unsafeToFuture().map(_ shouldBe List(3, 2))
  }

  test("concatenation") {
    val s1 = List(0, 1).toAS[IO]
    val s2 = List(2, 3).toAS[IO]
    val f = s1 <+> s2
    f.to[List].unsafeToFuture().map(_ shouldBe List(0, 1, 2, 3))
  }

  test("working as monad") {
    val s1 = List(0, 1).toAS[IO]
    val s2 = List(2, 3).toAS[IO]

    val res = for {
      v1 <- s1
      v2 <- s2
    } yield v1 * v2

    res.to[List].unsafeToFuture().map(_ shouldBe List(0, 0, 2, 3))
  }

  test("takeWhile") {
    val r = makeInfStream.takeWhile(_ < 4)
    r.to[List].unsafeToFuture().map(_ shouldBe List(0, 1, 2, 3))
  }

  test("take") {
    val r = makeInfStream.take(3)
    r.to[List].unsafeToFuture().map(_ shouldBe List(0, 1, 2))
  }

  test("folding large stream should not crash") {
    val r = makeInfStream.takeWhile(_ < 1000000)
    r.to[List].unsafeToFuture().map(_ shouldBe (0 to 999999))
  }

  test("foreach") {
    val stream = makeInfStream.take(10)
    val buffer = ArrayBuffer[Int]()
    val task = stream.foreach(i => buffer += i)
    Await.ready(task.unsafeToFuture(), 10.seconds)
    buffer.to[List] shouldBe 0 :: 1 :: 2 :: 3 :: 4 :: 5 :: 6 :: 7 :: 8 :: 9 :: Nil
  }

  test("foreachF") {
    val stream = makeInfStream.take(10)
    val buffer = ArrayBuffer[Int]()
    val task = stream.foreachF(i => IO(buffer += i))
    Await.ready(task.unsafeToFuture(), 10.seconds)
    buffer.to[List] shouldBe 0 :: 1 :: 2 :: 3 :: 4 :: 5 :: 6 :: 7 :: 8 :: 9 :: Nil
  }

  test("flatten") {
    val stream = Vector.range(0, 1000000).grouped(10).to[Vector].toAS[IO]
    val flatStream = stream.flatten
    flatStream.to[Vector].unsafeToFuture().map(_ shouldBe Vector.range(0, 1000000))
  }
}
