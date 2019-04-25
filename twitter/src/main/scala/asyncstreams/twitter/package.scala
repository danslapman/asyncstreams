package asyncstreams

import cats.{MonadError, MonoidK}
import com.twitter.util.Future
import com.twitter.util.Try.PredicateDoesNotObtain

package object twitter {
  implicit def futureEmptyKOrElse(implicit me: MonadError[Future, Throwable]): MonoidK[Future] = new MonoidK[Future] {
    override def empty[A]: Future[A] = me.raiseError(PredicateDoesNotObtain())

    override def combineK[A](x: Future[A], y: Future[A]): Future[A] =
      me.recoverWith(x) { case _: PredicateDoesNotObtain => y }
  }
}
