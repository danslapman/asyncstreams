package asyncstreams.monixTask

import asyncstreams.Utils._
import asyncstreams.{ASImpl, AsyncStream}
import asyncstreams.Implicits.MonadErrorInstances._
import monix.eval.Task
import monix.execution.Scheduler
import monix.scalaz._
import org.scalatest.{FunSuite, Matchers}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
import scalaz.syntax.monadPlus._

class AsyncStreamTests extends FunSuite with Matchers {
  import TaskZeroError.ze
  private implicit val scheduler = Scheduler.fixedPool("monix", 4)
  private def makeInfStream = AsyncStream.unfold[Task, Int](0)(_ + 1)
  private def wait[T](f: Task[T], d: FiniteDuration = 5.seconds): T = Await.result(f.runAsync, d)

  Task.never

  test("composition operator") {
    val s = 1 ~:: 2 ~:: 3 ~:: AsyncStream.asyncNil[Task, Int]
    wait(s.to[List]) shouldBe List(1, 2, 3)
  }

  test("foldLeft") {
    val s2 = List(2, 3).toAS[Task]
    val f = implicitly[ASImpl[Task]].collectLeft(s2)(List[Int]())((list, el) => el :: list)
    wait(f) shouldBe List(3, 2)
  }

  test("concatenation") {
    val s1 = List(0, 1).toAS[Task]
    val s2 = List(2, 3).toAS[Task]
    val f = s1 <+> s2
    wait(f.to[List]) shouldBe List(0, 1, 2, 3)
  }

  test("working as monad") {
    val s1 = List(0, 1).toAS[Task]
    val s2 = List(2, 3).toAS[Task]

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
    wait(r.to[List], 10.seconds) shouldBe (0 to 999999)
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
    val stream = Vector.range(0, 1000000).grouped(10).to[Vector].toAS[Task]
    val flatStream = stream.flatten
    wait(flatStream.to[Vector], 60.seconds) shouldBe Vector.range(0, 1000000)
  }
}
