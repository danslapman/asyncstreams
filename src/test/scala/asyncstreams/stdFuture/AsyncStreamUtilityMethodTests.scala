package asyncstreams.stdFuture

import asyncstreams._
import asyncstreams.impl._
import cats.instances.future._
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

class AsyncStreamUtilityMethodTests extends FunSuite with Matchers {
  private def await[T](f: Future[T], d: Duration = 5.seconds): T = Await.result(f, d)

  test("unfold") {
    val as = AsyncStream.unfold(0)(_ + 1).take(20)

    await(as.to[Vector]) shouldBe Vector.range(0, 20)
  }

  test("unfoldM") {
    val as = AsyncStream.unfoldM(0)(i => Future(i + 1)).take(20)

    await(as.to[Vector]) shouldBe Vector.range(0, 20)
  }

  test("munfoldM") {
    val as = AsyncStream.munfoldM(Future(0))(i => Future(i + 1)).take(20)

    await(as.to[Vector]) shouldBe Vector.range(0, 20)
  }
}
