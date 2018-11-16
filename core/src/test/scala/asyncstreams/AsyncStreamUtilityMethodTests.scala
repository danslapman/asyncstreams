package asyncstreams

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{CopyOnWriteArrayList, Executors}

import cats.Eval
import cats.instances.future._
import org.scalatest.{AsyncFunSuite, Matchers}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class AsyncStreamUtilityMethodTests extends AsyncFunSuite with Matchers with TestHelpers {
  override implicit def executionContext: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  test("unfold") {
    val as = AsyncStream.unfold[Future, Int](0)(_ + 1).take(20)

    as.to[Vector].map(_ shouldBe Vector.range(0, 20))
  }

  test("unfoldM") {
    val as: AsyncStream[Future, Int] = AsyncStream.unfoldM(0)(i => Future(i + 1)).take(20)

    as.to[Vector].map(_ shouldBe Vector.range(0, 20))
  }

  test("unfoldMM") {
    val as: AsyncStream[Future, Int] = AsyncStream.unfoldMM(Future(0))(i => Future(i + 1)).take(20)

    as.to[Vector].map(_ shouldBe Vector.range(0, 20))
  }

  test("unfoldM with foreach") {
    val output = new CopyOnWriteArrayList[String]()

    def emitNext(i: Int) = {
      output.add(s"Emit $i")
      Future.successful(i)
    }

    val as: AsyncStream[Future, Int] = AsyncStream.unfoldM(0)(i => emitNext(i + 1)).take(20)

    Thread.sleep(100)
    output.asScala shouldBe List("Emit 1")

    as.foreach { i =>
      Thread.sleep(100)
      output.add(s"Receive $i")
    }.map { _ =>
      output.asScala shouldBe List.range(1, 21).flatMap(i => List(s"Emit $i", s"Receive ${i - 1}")) ::: List("Emit 21")
    }
  }

  test("unfoldM with foreachF") {
    val output = new CopyOnWriteArrayList[String]()

    def emitNext(i: Int) = {
      output.add(s"Emit $i")
      Future.successful(i)
    }

    val as: AsyncStream[Future, Int] = AsyncStream.unfoldM(0)(i => emitNext(i + 1)).take(20)

    Thread.sleep(100)
    output.asScala shouldBe List("Emit 1")

    as.foreachF { i =>
      Future.successful {
        Thread.sleep(100)
        output.add(s"Receive $i")
      }
    }.map { _ =>
      output.asScala shouldBe List.range(1, 21).flatMap(i => List(s"Emit $i", s"Receive ${i - 1}")) ::: List("Emit 21")
    }
  }

  test("continually") {
    val i = new AtomicInteger(0)

    val as = AsyncStream.continually(i.getAndIncrement()).take(20)

    as.to[Vector].map(_ shouldBe Vector.range(0, 20))
  }

  test("continuallyF") {
    val i = new AtomicInteger(0)

    def next = Future.successful(i.getAndIncrement())

    val as = AsyncStream.continuallyF(next).take(20)

    as.to[Vector].map(_ shouldBe Vector.range(0, 20))
  }

  test("continuallyEval later") {
    val i = new AtomicInteger(0)

    val as = AsyncStream.continuallyEval(Eval.later(i.getAndIncrement())).take(20)

    as.to[Vector].map(_ shouldBe Vector.fill(20)(0))
  }

  test("continuallyEval always") {
    val i = new AtomicInteger(0)

    val as = AsyncStream.continuallyEval(Eval.always(i.getAndIncrement())).take(20)

    as.to[Vector].map(_ shouldBe Vector.range(0, 20))
  }
}