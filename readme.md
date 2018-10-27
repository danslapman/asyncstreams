asyncstreams [ ![Download](https://api.bintray.com/packages/danslapman/maven/asyncstreams/images/download.svg) ](https://bintray.com/danslapman/maven/asyncstreams/_latestVersion)
=========

asyncstreams is a asynchronous pull-based stream library. It allows you to write stateful asynchronous algorithms
that emits elements into a stream:

```scala
val stream = genS(0) {
      for {
        s <- getS[Int]
        if s < 3
        _ <- putS(s + 1)
      } yield s
    }

stream.to[List].map(_ shouldBe (0 :: 1 :: 2 :: Nil))
```

See more examples in tests.

asyncstreams is tested to work with:
- standard scala futures
- twitter futures
- any other effect for which EmptyKOrElse can be implemented

asyncstreams is available via bintray:

```
    resolvers += Resolver.bintrayRepo("danslapman", "maven")

    libraryDependencies += "danslapman" %% "asyncstreams" % "3.0.0"
```
