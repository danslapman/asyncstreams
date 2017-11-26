package asyncstreams.stdFuture

import asyncstreams._
import asyncstreams.impl._
import cats.instances.future._
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

class AsyncStreamOperations extends FunSuite with Matchers {
  private def await[T](f: Future[T], d: Duration = 5.seconds): T = Await.result(f, d)

  private def stream = (0 to 30).toAS[Future]

  test("map") {
    import cats.syntax.functor._

    val res = stream.map(_ * 2).to[Vector]

    await(res) shouldBe (0 to 60 by 2)
  }
}
