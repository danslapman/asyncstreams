val versions = Map(
  "monix" -> "3.0.0-RC1",
  "cats" -> "1.2.0",
  "twitter" -> "18.7.0"
)
/*
libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % versions("cats"),
  "org.typelevel" %% "alleycats-core" % versions("cats"),
  "org.typelevel" %% "cats-mtl-core" % "0.3.0",
  "com.github.mpilquist" %% "simulacrum" % "0.13.0",
  "org.typelevel" %% "cats-effect" % "1.0.0-RC2" % Test,
  "io.monix" %% "monix-eval" % versions("monix") % Test,
  "com.twitter" %% "util-core" % versions("twitter") % Test,
  "io.catbird" %% "catbird-util" % versions("twitter") % Test,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)*/

lazy val asyncstreams = (project in file("core"))
  .aggregate(`asyncstreams-twitter`)
  .settings(Settings.common)
  .settings(
    name := "asyncstreams",
    parallelExecution in ThisBuild := false,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % versions("cats"),
      "org.typelevel" %% "alleycats-core" % versions("cats"),
      "org.typelevel" %% "cats-mtl-core" % "0.3.0",
      "com.github.mpilquist" %% "simulacrum" % "0.13.0",
      "org.scalatest" %% "scalatest" % "3.0.5" % Test
    )
  )

lazy val asyncstreamsRef = LocalProject("asyncstreams")

lazy val `asyncstreams-twitter` = (project in file("twitter"))
  .dependsOn(asyncstreamsRef)
  .settings(Settings.common)
  .settings(
    name := "asyncstreams",
    parallelExecution in ThisBuild := false,
    libraryDependencies ++= Seq(
      "com.twitter" %% "util-core" % versions("twitter"),
      "io.catbird" %% "catbird-util" % versions("twitter") % Test,
      "org.scalatest" %% "scalatest" % "3.0.5" % Test
    )
  )