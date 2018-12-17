val versions = Map(
  "cats" -> "1.5.0",
  "twitter" -> "18.7.0"
)

lazy val asyncstreams = (project in file("core"))
  .aggregate(`asyncstreams-twitter`)
  .settings(Settings.common)
  .settings(
    name := "asyncstreams",
    parallelExecution in ThisBuild := false,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % versions("cats"),
      "org.typelevel" %% "alleycats-core" % versions("cats"),
      "org.typelevel" %% "cats-mtl-core" % "0.4.0",
      "com.github.mpilquist" %% "simulacrum" % "0.13.0",
      "org.scalatest" %% "scalatest" % "3.0.5" % Test
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4")
  )

lazy val asyncstreamsRef = LocalProject("asyncstreams")

lazy val `asyncstreams-twitter` = (project in file("twitter"))
  .dependsOn(asyncstreamsRef % "test->test;compile->compile")
  .settings(Settings.common)
  .settings(
    name := "asyncstreams-twitter",
    parallelExecution in ThisBuild := false,
    libraryDependencies ++= Seq(
      "com.twitter" %% "util-core" % versions("twitter"),
      "io.catbird" %% "catbird-util" % versions("twitter") % Test,
      "org.scalatest" %% "scalatest" % "3.0.5" % Test
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