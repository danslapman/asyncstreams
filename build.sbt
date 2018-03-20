name := "asyncstreams"

organization := "danslapman"

version := "1.0.1"

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.11.12", "2.12.4")

scalacOptions += "-Ypartial-unification"

parallelExecution in ThisBuild := false

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6")

val versions = Map(
  "monix" -> "3.0.0-RC1",
  "cats" -> "1.0.0"
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % versions("cats"),
  "org.typelevel" %% "alleycats-core" % versions("cats"),
  "org.typelevel" %% "cats-mtl-core" % "0.2.3",
  "org.typelevel" %% "cats-effect" % "0.10" % Test,
  "io.monix" %% "monix-eval" % versions("monix") % Test,
  "com.twitter" %% "util-core" % "18.3.0" % Test,
  "io.catbird" %% "catbird-util" % "18.3.0" % Test,
  "org.scalatest" %% "scalatest" % "3.0.4" % Test
)

licenses += ("WTFPL", url("http://www.wtfpl.net"))

bintrayOrganization := Some("danslapman")

bintrayReleaseOnPublish in ThisBuild := false