name := "asyncstreams"

version := "1.0"

scalaVersion := "2.12.4"

parallelExecution in ThisBuild := false

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")

val versions = Map(
  "monix" -> "2.3.2"
)

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.2.16",
  "io.monix" %% "monix-eval" % versions("monix") % Test,
  "io.monix" %% "monix-scalaz-72" % versions("monix") % Test,
  "com.twitter" %% "util-core" % "7.1.0" % Test,
  "io.catbird" %% "catbird-util" % "0.18.0" % Test,
  "me.jeffshaw.harmony" %% "harmony_cats1-0-0-mf_scalaz7-2" % "2.0" % Test,
  "org.scalatest" %% "scalatest" % "3.0.4" % Test
)