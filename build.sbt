name := "asyncstreams"

organization := "danslapman"

version := "1.0-rc1"

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.11.12", "2.12.4")

scalacOptions += "-Ypartial-unification"

parallelExecution in ThisBuild := false

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.5")

val versions = Map(
  "monix" -> "3.0.0-M2",
  "cats" -> "1.0.0-RC1"
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % versions("cats"),
  "org.typelevel" %% "alleycats-core" % versions("cats"),
  "org.typelevel" %% "cats-mtl-core" % "0.1.0",
  "io.monix" %% "monix-eval" % versions("monix") % Test,
  "com.twitter" %% "util-core" % "17.11.0" % Test,
  "io.catbird" %% "catbird-util" % "0.21.0" % Test,
  "org.scalatest" %% "scalatest" % "3.0.4" % Test
)

licenses += ("WTFPL", url("http://www.wtfpl.net"))

bintrayOrganization := Some("danslapman")

bintrayReleaseOnPublish in ThisBuild := false