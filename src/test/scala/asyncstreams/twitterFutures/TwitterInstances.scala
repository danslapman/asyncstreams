package asyncstreams.twitterFutures

import com.twitter.util.Future

import scalaz.{Functor, Monad}

object TwitterInstances {
  implicit val FutureFunctor = new Functor[Future] {
    def map[A, B](a: Future[A])(f: A => B): Future[B] = a map f
  }
  implicit val FutureMonad = new Monad[Future] {
    def point[A](a: => A): Future[A] = Future(a)
    def bind[A, B](fa: Future[A])(f: (A) => Future[B]): Future[B] = fa flatMap f
  }
}
