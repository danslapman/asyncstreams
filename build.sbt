name := "asyncstreams"

version := "1.0"

scalaVersion := "2.12.3"

parallelExecution in ThisBuild := false

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")

val versions = Map(
  "monix" -> "2.3.0"
)

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.2.15",
  "io.monix" %% "monix-eval" % versions("monix") % Test,
  "io.monix" %% "monix-scalaz-72" % versions("monix") % Test,
  //"com.twitter" %% "util-core" % "6.43.0" % Test,
  //"io.catbird" %% "catbird-util" % "0.14.0" % Test, //cats instances for util-core
  //"com.codecommit" %% "shims-core" % "1.0-b0e5152" % Test,
  "org.scalatest" %% "scalatest" % "3.0.3" % Test
)