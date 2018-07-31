package asyncstreams

import cats.MonadError
import com.twitter.util.Future
import com.twitter.util.Try.PredicateDoesNotObtain

package object twitter {
  implicit def futureEmptyKOrElse(implicit me: MonadError[Future, Throwable]): EmptyKOrElse[Future] = new EmptyKOrElse[Future] {
    override def empty[A]: Future[A] = me.raiseError(PredicateDoesNotObtain())

    override def orElse[A](fa: Future[A], default: => Future[A]): Future[A] =
      me.recoverWith(fa) { case _: PredicateDoesNotObtain  => default }
  }
}
