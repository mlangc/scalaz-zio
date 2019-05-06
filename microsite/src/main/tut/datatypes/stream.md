---
layout: docs
section: datatypes
title:  "Stream"
---

# {{page.title}}

## Setup
Stream related APIs are imported by
```scala mdoc:silent
import scalaz.zio._
import scalaz.zio.stream._
```
and come with the streams module, that has to be added as additional dependency. If you are using SBT, this can be done
using 
```scala mdoc:passthrough
println(s"""```""")
if (scalaz.zio.BuildInfo.isSnapshot)
  println(s"""resolvers += Resolver.sonatypeRepo("snapshots")""")
println(s"""libraryDependencies += "org.scalaz" %% "calaz-zio-streams" % "${scalaz.zio.BuildInfo.version}"""")
println(s"""```""")
```

## Abstract
A value of type `ZStream[R, E, A]` describes an effectful stream of values of type `A`, that might fail with an error
`E` and requires an environment `R`. Streams that don't need a specific environment are modeled by the type alias

```scala mdoc:silent
type Stream[+E, +A] = ZStream[Any, E, A]
```

Having an API that is similar to the standard Scala collections, streams should feel familiar to most developers. Unlike
the latter, they can be used to model data arriving from the network, files, random numbers and so forth.

## Creating Pure Streams
Apart from creating streams directly, by implementing `ZStream.fold`, which will be discussed later, there are various 
factory methods on the `ZStream` companion object that help you creating pure streams. The simplest example is certainly 
the stream that never emits a value:

```scala mdoc:silent
val empty: Stream[Nothing, Nothing] = ZStream.empty
```

Here are a few slightly more complicated examples:

```scala mdoc:silent
val singleton: Stream[Nothing, Int] = ZStream.succeed(1)
val oneTwo: Stream[Nothing, Int] = ZStream(1, 2)
val forever21: Stream[Nothing, Int] = ZStream.succeed(21).forever
val fromList: Stream[Nothing, Int] = ZStream.fromIterable(List(42, 43, 44))
```

A stream of Fibonacci numbers can be summoned like this:

```scala mdoc:silent
val fibonacci: Stream[Nothing, Int] = ZStream.unfold((0, 1)) { case (f0, f1) =>
  Some((f1, (f1, f0 + f1)))
}
```

## Consuming Streams
Streams themselves are nothing but blueprints. To actually use them, they have to be consumed. This is what Sinks are
for. One of the most useful Sink implementations simply collects the entire Stream into a List:

```scala mdoc:silent
val stream1: Stream[Nothing, Int] = Stream(1, 2, 3)
val sink1: Sink[Nothing, Nothing, Int, List[Int]] = Sink.collect[Int]
val list1: IO[Nothing, List[Int]] = stream1.run(sink1)
```
Another possibility is folding over the elements:

```scala mdoc:silent
val stream2: Stream[Nothing, Int] = Stream(1, 2, 3)
val sink2 = Sink.foldLeft(0)((_: Int) + (_: Int))
val sum: IO[Nothing, Int] = stream2.run(sink2)
```

For infinite streams, `readWhile` can be used instead - either on `Sink`, as in

```scala mdoc:silent
val stream3: Stream[Nothing, Int] = Stream.succeed(1) ++ Stream.succeed(2).forever
val sink3 = Sink.readWhile[Int](_ < 2)
val elemsSmaller2: IO[Nothing, List[Int]] = stream3.run(sink3)
```

or on the `Stream` before running it

```scala mdoc:silent
val alsoSmaller2: IO[Nothing, List[Int]] = stream3.takeWhile(_ < 2).run(Sink.collect[Int])
```


