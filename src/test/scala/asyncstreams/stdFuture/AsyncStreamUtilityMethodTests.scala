package asyncstreams.stdFuture

import asyncstreams._
import cats.instances.future._
import org.scalatest.{AsyncFunSuite, Matchers}

import scala.concurrent.Future

class AsyncStreamUtilityMethodTests extends AsyncFunSuite with Matchers {
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
}
