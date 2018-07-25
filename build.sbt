name := "asyncstreams"

organization := "danslapman"

version := "2.0.0-SNAPSHOT"

scalaVersion := "2.12.6"

crossScalaVersions := Seq("2.11.12", "2.12.6")

scalacOptions += "-Ypartial-unification"

parallelExecution in ThisBuild := false

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

val versions = Map(
  "monix" -> "3.0.0-RC1",
  "cats" -> "1.1.0",
  "twitter" -> "18.6.0"
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % versions("cats"),
  "org.typelevel" %% "alleycats-core" % versions("cats"),
  "org.typelevel" %% "cats-mtl-core" % "0.3.0",
  "org.typelevel" %% "cats-effect" % "1.0.0-RC2" % Test,
  "io.monix" %% "monix-eval" % versions("monix") % Test,
  "com.twitter" %% "util-core" % versions("twitter") % Test,
  "io.catbird" %% "catbird-util" % versions("twitter") % Test,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)

licenses += ("WTFPL", url("http://www.wtfpl.net"))

bintrayOrganization := Some("danslapman")

bintrayReleaseOnPublish in ThisBuild := false