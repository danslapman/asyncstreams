package asyncstreams.stdFuture

import asyncstreams._
import asyncstreams.impl._
import cats.instances.future._
import cats.syntax.applicative._
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

class AsyncStreamOperations extends FunSuite with Matchers {
  private def await[T](f: Future[T], d: Duration = 5.seconds): T = Await.result(f, d)

  private def stream = (0 to 30).toAS[Future]
  private def longStream = AsyncStream.unfold[Future, Int](0)(_ + 1).take(100000)

  test("map") {
    val res = stream.map(_ * 2).to[Vector]

    await(res) shouldBe (0 to 60 by 2)
  }

  test("map lang stream") {
    val res = longStream.map(_ * 2).to[Vector]

    await(res) should have length 100000
  }

  test("mapF") {
    val res = stream.mapF(v => (v * 2).pure[Future]).to[Vector]

    await(res) shouldBe (0 to 60 by 2)
  }

  test("mapF long stream") {
    val res = longStream.mapF(v => (v * 2).pure[Future]).to[Vector]

    await(res) should have length 100000
  }

  test("flatMap") {
    val res = stream.map(_ * 2).flatMap(v => v ~:: (v + 1) ~:: AsyncStream.asyncNil[Future, Int]).to[Vector]

    await(res) shouldBe (0 to 61)
  }

  test("flatMap long stream") {
    val res = longStream.map(_ * 2).flatMap(v => v ~:: (v + 1) ~:: AsyncStream.asyncNil[Future, Int]).to[Vector]

    await(res) should have length 200000
  }

  test("filter") {
    val res = stream.filter(_ % 2 == 0).to[Vector]

    await(res) shouldBe (0 to 30 by 2)
  }

  test("filter long stream") {
    val res = longStream.filter(_ % 2 == 0).to[Vector]

    await(res) should have length 50000
  }
}
