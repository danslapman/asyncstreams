name := "asyncstreams"

version := "0.4"

scalaVersion := "2.11.8"

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.2.8",
  "com.twitter" %% "util-core" % "6.41.0" % Test,
  "org.scalatest" %% "scalatest" % "3.0.1" % Test
)