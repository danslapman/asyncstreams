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

Await.result(stream.to[List]) shouldBe (0 :: 1 :: 2 :: Nil)
```

See more examples in tests.

asyncstreams is tested to work with:
- standard scala futures
- twitter futures
- monix tasks

Currently, asyncstreams' main branch uses scalaz's typeclasses. If Your projects uses cats
or have some other cats-based dependencies, You can use cats-based port, which have feature parity
with master, but is experimental for now.

asyncstreams is available via jitpack:

```
    resolvers += "jitpack" at "https://jitpack.io"

    libraryDependencies += "com.github.danslapman" %% "asyncstreams" % "0.5"
    
    //or if You need cats:
    libraryDependencies += "com.github.danslapman" %% "asyncstreams" % "0.5-cats"
```

asyncstreams initially based on [scala-async](https://github.com/iboltaev/scala-async) ideas.