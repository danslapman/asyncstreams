package asyncstreams.monixTasks

import asyncstreams._
import asyncstreams.AsyncStream._
import asyncstreams.BaseSuite
import monix.eval.Task
import monix.execution.Scheduler
import monix.scalaz._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
import scalaz.syntax.monad._
import scalaz.syntax.std.boolean._

class MonixTaskAsyncStreamTests extends BaseSuite {
  private implicit val scheduler = Scheduler.fixedPool("monix", 4)
  private def makeStream[T](l: Iterable[T]) = generate(l)(l => (l.nonEmpty ?(l.head, l.tail)|END).point[Task])
  private def makeInfStream = generate(0)(v => ((v, v + 1)).point[Task])
  private def wait[T](f: Task[T]): T = Await.result(f.runAsync, 30.seconds)

  test("foldLeft") {
    val s2 = makeStream(2 :: 3 :: Nil)
    val f = s2.foldLeft(List[Int]())((list, el) => el :: list)
    wait(f) shouldBe List(3, 2)
  }

  test("concat") {
    val s1 = makeStream(0 :: 1 :: Nil)
    val s2 = makeStream(2 :: 3 :: Nil)
    val f = concat(s1, s2)
    wait(f.to[List]) shouldBe List(0, 1, 2, 3)
  }

  test("working as monad") {
    val s1 = makeStream(0 :: 1 :: Nil)
    val s2 = makeStream(2 :: 3 :: Nil)

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
    wait(r.to[List]) shouldBe (0 to 999999)
  }

  test("foreach") {
    val stream = makeInfStream.take(10)
    val buffer = ArrayBuffer[Int]()
    val task = stream.foreach(i => buffer += i)
    Await.ready(task.runAsync, 10.seconds)
    buffer.to[List] shouldBe 0 :: 1 :: 2 :: 3 :: 4 :: 5 :: 6 :: 7 :: 8 :: 9 :: Nil
  }

  test("foreachF") {
    val stream = makeInfStream.take(10)
    val buffer = ArrayBuffer[Int]()
    val task = stream.foreachF(i => (buffer += i).point[Task])
    Await.ready(task.runAsync, 10.seconds)
    buffer.to[List] shouldBe 0 :: 1 :: 2 :: 3 :: 4 :: 5 :: 6 :: 7 :: 8 :: 9 :: Nil
  }

  test("flatten") {
    val stream = makeStream(Vector.range(0, 1000000).grouped(10).to[Vector])
    val flatStream = stream.flatten
    wait(flatStream.to[Vector]) shouldBe Vector.range(0, 1000000)
  }
}
