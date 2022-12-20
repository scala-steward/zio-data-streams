import Dependencies._
import sbt.Keys.libraryDependencies

ThisBuild / scalaVersion     := "3.2.1"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.gregorpurdy"
ThisBuild / organizationName := "Gregor Purdy"
ThisBuild / startYear        := Some(2022)
ThisBuild / licenses         := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

lazy val root = (project in file("."))
  .settings(
    name := "zdata",
    scalacOptions ++= Seq("-feature", "-deprecation"),
    libraryDependencies ++= Seq(
      zioStreams      % Compile,
      zioTest         % Test,
      zioTestSbt      % Test,
      "commons-codec" % "commons-codec" % "1.15" % Test
    )
  )
