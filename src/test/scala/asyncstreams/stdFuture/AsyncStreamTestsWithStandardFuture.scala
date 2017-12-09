package asyncstreams.stdFuture

import java.util.concurrent.Executors

import asyncstreams._
import asyncstreams.impl._
import asyncstreams.{ASImpl, AsyncStream}
import cats.Monad
import cats.instances.future._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.semigroupk._
import org.scalatest.{FunSuite, Matchers}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class AsyncStreamTestsWithStandardFuture extends FunSuite with Matchers {
  private implicit val executor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  private def makeInfStream: AsyncStream[Future, Int] = AsyncStream.unfold(0)(_ + 1)
  private def wait[T](f: Future[T], d: FiniteDuration = 5.seconds): T = Await.result(f, d)

  test("composition operator") {
    val s = 1 ~:: 2 ~:: 3 ~:: AsyncStream.asyncNil[Future, Int]
    wait(s.to[List]) shouldBe List(1, 2, 3)
  }

  test("foldLeft") {
    val s2 = List(2, 3).toAS[Future]
    val f = implicitly[ASImpl[Future]].collectLeft(s2)(List[Int]())((list, el) => el :: list)
    wait(f) shouldBe List(3, 2)
  }

  test("concatenation") {
    val s1 = List(0, 1).toAS[Future]
    val s2 = List(2, 3).toAS[Future]
    val f = s1 <+> s2
    wait(f.to[List]) shouldBe List(0, 1, 2, 3)
  }

  test("working as monad") {
    val s1 = List(0, 1).toAS[Future]
    val s2 = List(2, 3).toAS[Future]

    val res = for {
      v1 <- s1
      v2 <- s2
    } yield v1 * v2

    wait(res.to[List]) shouldBe List(0, 0, 2, 3)
  }

  test("takeWhile") {
    val r = makeInfStream.takeWhile(_ < 4)
    wait(r.to[List]) shouldBe List(0, 1, 2, 3)
  }

  test("take") {
    val r = makeInfStream.take(3)
    wait(r.to[List]) shouldBe List(0, 1, 2)
  }

  test("folding large stream should not crash") {
    val r = makeInfStream.takeWhile(_ < 1000000)
    wait(r.to[List], 20.seconds) shouldBe (0 to 999999)
  }

  test("foreach") {
    val stream = makeInfStream.take(10)
    val buffer = ArrayBuffer[Int]()
    val task = stream.foreach(i => buffer += i)
    Await.ready(task, 10.seconds)
    buffer.to[List] shouldBe 0 :: 1 :: 2 :: 3 :: 4 :: 5 :: 6 :: 7 :: 8 :: 9 :: Nil
  }

  test("foreachF") {
    val stream = makeInfStream.take(10)
    val buffer = ArrayBuffer[Int]()
    val task = stream.foreachF(i => Future(buffer += i))
    Await.ready(task, 10.seconds)
    buffer.to[List] shouldBe 0 :: 1 :: 2 :: 3 :: 4 :: 5 :: 6 :: 7 :: 8 :: 9 :: Nil
  }

  test("flatten") {
    val stream = Vector.range(0, 1000000).grouped(10).to[Vector].toAS[Future]
    val flatStream = stream.flatten
    wait(flatStream.to[Vector], 20.seconds) shouldBe Vector.range(0, 1000000)
  }
}
