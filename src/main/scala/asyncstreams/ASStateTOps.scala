package asyncstreams

/*
import scala.language.higherKinds
import scalaz.syntax.monad._
import scalaz.{IndexedStateT, Monad, MonadPlus, MonadState, StateT}

class ASStateTOps[F[+_]: Monad](implicit methods: ASImpl[F]) {
  def foreach[A, S](stream: AsyncStream[F, A])(f: A => StateT[F, S, _]): StateT[F, S, Unit] = StateT { s =>
    methods.collectLeft(stream)(s.point[F])((fS, a) => fS.flatMap(s2 => f(a)(s2).map(_._1)))
      .flatMap(identity).map((_, ()))
  }

  def isEmpty[A, S](stream: AsyncStream[F, A]): StateT[F, S, Boolean] = StateT { s =>
    stream.isEmpty.map((s, _))
  }

  def isEmpty[A, S](f: S => AsyncStream[F, A])(implicit ms: MonadState[IndexedStateT[F, S, S, ?], S]): StateT[F, S, Boolean] = {
    ms.get >>= ((s: S) => isEmpty(f(s)))
  }

  def notEmpty[A, S](stream: AsyncStream[F, A]): StateT[F, S, Boolean] = StateT { s =>
    stream.nonEmpty.map((s, _))
  }

  def notEmpty[A, S](f: S => AsyncStream[F, A])(implicit ms: MonadState[IndexedStateT[F, S, S, ?], S]): StateT[F, S, Boolean] = {
    ms.get >>= ((s: S) => notEmpty(f(s)))
  }

  def get[A, S](stream: AsyncStream[F, A]): StateT[F, S, (AsyncStream[F, A], A)] = StateT { s =>
    stream.data.map(step => (s, (step.rest, step.value)))
  }

  def genS[S, A](start: S)(gen: StateT[F, S, A])(implicit smp: MonadPlus[AsyncStream[F, ?]]): AsyncStream[F, A] =
    AsyncStream.generate(start)(gen.run)
}
*/