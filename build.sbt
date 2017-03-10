name := "asyncstreams"

version := "0.5-SNAPSHOT"

scalaVersion := "2.11.8"

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")

val versions = Map(
  "monix" -> "2.2.3"
)

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.2.9",
  "com.twitter" %% "util-core" % "6.41.0" % Test,
  "io.monix" %% "monix-eval" % versions("monix") % Test,
  "io.monix" %% "monix-scalaz-72" % versions("monix") % Test,
  "org.scalatest" %% "scalatest" % "3.0.1" % Test
)