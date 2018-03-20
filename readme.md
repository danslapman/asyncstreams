asyncstreams [ ![Download](https://api.bintray.com/packages/danslapman/maven/asyncstreams/images/download.svg) ](https://bintray.com/danslapman/maven/asyncstreams/_latestVersion)
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
- cats-effect IO

asyncstreams is available via bintray:

```
    resolvers += Resolver.bintrayRepo("danslapman", "maven")

    libraryDependencies += "danslapman" %% "asyncstreams" % "1.0.1"
```

asyncstreams initially based on [scala-async](https://github.com/iboltaev/scala-async) ideas.