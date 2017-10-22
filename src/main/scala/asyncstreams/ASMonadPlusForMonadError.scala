package asyncstreams

import asyncstreams.typeclass.ZeroK

import scala.language.higherKinds
import scalaz.syntax.monadError._
import scalaz.{MonadError, MonadPlus}

class ASMonadPlusForMonadError[F[+_]](implicit fmp: MonadError[F, Throwable], zk: ZeroK[F]) extends MonadPlus[AsyncStream[F, ?]] {
  override def bind[A, B](fa: AsyncStream[F, A])(f: (A) => AsyncStream[F, B]): AsyncStream[F, B] = AsyncStream {
    fa.data.flatMap(step => f(step.value).data.map(step2 => Step(step2.value, plus(step2.rest, bind(step.rest)(f)))))
    .handleError(_ => zk.zero)
  }

  override def plus[A](a: AsyncStream[F, A], b: => AsyncStream[F, A]): AsyncStream[F, A] = AsyncStream {
    a.data.map(step => Step(step.value, plus(step.rest, b))).handleError(_ => b.data)
  }

  override def point[A](a: => A): AsyncStream[F, A] = AsyncStream(Step(a, empty[A]).point[F])

  override def empty[A]: AsyncStream[F, A] = AsyncStream(zk.zero)
}
