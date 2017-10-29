name := "asyncstreams"

version := "1.0"

scalaVersion := "2.12.4"

scalacOptions += "-Ypartial-unification"

parallelExecution in ThisBuild := false

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")

val versions = Map(
  "monix" -> "3.0.0-M1"
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.0.0-MF",
  "org.typelevel" %% "cats-mtl-core" % "0.0.2",
  "io.monix" %% "monix-eval" % versions("monix") % Test,
  "com.twitter" %% "util-core" % "7.1.0" % Test,
  "io.catbird" %% "catbird-util" % "0.18.0" % Test,
  "org.scalatest" %% "scalatest" % "3.0.4" % Test
)