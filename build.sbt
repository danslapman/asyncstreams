name := "asyncstreams"

version := "0.4-SNAPSHOT"

scalaVersion := "2.11.8"

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.2.8",
  "org.scalatest" %% "scalatest" % "3.0.1" % Test
)