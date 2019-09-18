val versions = Map(
  "cats" -> "2.0.0",
  "twitter" -> "19.6.0",
  "scalatest" -> "3.0.8"
)

lazy val asyncstreams = (project in file("core"))
  .settings(Settings.common)
  .settings(
    name := "asyncstreams",
    parallelExecution in ThisBuild := false,
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.2",
      "org.typelevel" %% "cats-core" % versions("cats"),
      "org.typelevel" %% "alleycats-core" % versions("cats"),
      "org.typelevel" %% "cats-mtl-core" % "0.7.0",
      "com.github.mpilquist" %% "simulacrum" % "0.19.0",
      "org.scalatest" %% "scalatest" % versions("scalatest") % Test
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
  )

lazy val asyncstreamsRef = LocalProject("asyncstreams")

lazy val `asyncstreams-twitter` = (project in file("twitter"))
  .dependsOn(asyncstreamsRef)
  .settings(Settings.common)
  .settings(
    name := "asyncstreams-twitter",
    scalaVersion := "2.12.10",
    crossScalaVersions := Seq("2.11.12", "2.12.10"),
    parallelExecution in ThisBuild := false,
    libraryDependencies ++= Seq(
      "com.twitter" %% "util-core" % versions("twitter"),
      "io.catbird" %% "catbird-util" % versions("twitter") % Test,
      "org.scalatest" %% "scalatest" % versions("scalatest") % Test
    )
  )

lazy val root = (project in file("."))
  .dependsOn(asyncstreams, `asyncstreams-twitter`)
  .aggregate(asyncstreams, `asyncstreams-twitter`)
  .settings(
    publish := {},
    bintrayRelease := {},
    bintrayUnpublish := {}
  )