package asyncstreams.twitterFuture

import asyncstreams.typeclass.ZeroK
import com.twitter.util.Future

object TwitterFutureZeroK {
  implicit val zeroK = new ZeroK[Future] {
    override def zero[A] = Future.exception(new NoSuchElementException)
  }
}
