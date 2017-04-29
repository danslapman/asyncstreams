name := "asyncstreams"

version := "1.0"

scalaVersion := "2.12.2"

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")

val versions = Map(
  "monix" -> "2.2.4"
)

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.2.11",
  "com.twitter" %% "util-core" % "6.43.0" % Test,
  "org.scalatest" %% "scalatest" % "3.0.3" % Test
)