package asyncstreams.twitterFuture

import asyncstreams.ZeroError
import com.twitter.util.Future
import io.catbird.util.FutureInstances
import harmony.toscalaz.typeclass.MonadErrorConverter._

object TwFutureZeroError extends FutureInstances {
  implicit val ze = new ZeroError[Throwable, Future] {
    override val zeroElement: Throwable = new NoSuchElementException
  }
}
