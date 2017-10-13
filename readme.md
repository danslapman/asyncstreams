asyncstreams [![Release](https://jitpack.io/v/danslapman/asyncstreams.svg)](https://jitpack.io/#danslapman/asyncstreams)
=========

asyncstreams is a monadic asynchronous stream library. It allows you to write stateful asynchronous algorithms
that emits elements into a stream:

```scala
val stream = genS(0) {
      for {
        s <- getS[Int]
        if s < 3
        _ <- putS(s + 1)
      } yield s
    }

wait(stream.to[List]) shouldBe (0 :: 1 :: 2 :: Nil)
```

See more examples in tests.

asyncstreams is tested to work with:
- standard scala futures
- twitter futures
- monix tasks

asyncstreams is available via jitpack:

```
    resolvers += "jitpack" at "https://jitpack.io"

    libraryDependencies += "com.github.danslapman" %% "asyncstreams" % "master-SNAPSHOT"
```

asyncstreams initially based on [scala-async](https://github.com/iboltaev/scala-async) ideas.