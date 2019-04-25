asyncstreams [ ![Download](https://api.bintray.com/packages/danslapman/maven/asyncstreams/images/download.svg) ](https://bintray.com/danslapman/maven/asyncstreams/_latestVersion)
============

AsyncStream is a List-like data structure, which is both lazy (like Stream) and asynchronous.

#### Creating AsyncStream

```scala
// Simpliest way, just like Stream
val s1 = 1 ~:: 2 ~:: 3 ~:: ANil[Future, Int]

// Infinite stream starting with 0
val s2 = AsyncStream.unfold[Future, Int](0)(_ + 1)

// Same, but `makeNext` returns Future
val s3 = AsyncStream.unfoldM[Future, Int](0)(i => Future(i + 1))

// Same, but initial value is Future
val s4 = AsyncStream.unfoldMM[Future, Int](Future(0))(i => Future(i + 1))

// Covert iterable into AsyncStream
val s5 = AsyncStream.fromIterable[Future, Int](List.range(0, 50))

// Same as above, but using extension method
val s6 = List.range(0, 50).toAS[Future, Int]
```

#### Consuming AsyncStream

```scala
// If process function is synchronous, use foreach
// foreach receives A => Something
stream.foreach { i =>
    process(i)
}

// If You want to process elements asynchronously,
// You can use foreachF
// foreachF receives A => F[Something]
stream.foreachF { i =>
    process(i)
}

```

You can write stateful asynchronous algorithms that emits elements into a stream:

```scala
val stream = genS(0) {
  for {
     s <- getS[Int]
     if s < 3
     _ <- putS(s + 1)
   } yield s
 }

stream.foreach(println)
// Output:
//0
//1
//2
//3
```

See more examples in tests.

asyncstreams is tested to work with:
- standard scala futures
- twitter futures
- any other effect for which MonoidK can be implemented

asyncstreams is available via bintray:

```
    resolvers += Resolver.bintrayRepo("danslapman", "maven")

    libraryDependencies += "danslapman" %% "asyncstreams" % "4.0.0"
```
