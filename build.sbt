import Dependencies._
import sbt.Keys.libraryDependencies

lazy val scala3                 = "3.2.1"
lazy val scala213               = "2.13.10"
lazy val scala212               = "2.12.17"
lazy val scala211               = "2.11.12"
lazy val supportedScalaVersions = List(scala3)

ThisBuild / scalaVersion     := scala3
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.gregorpurdy"
ThisBuild / organizationName := "Gregor Purdy"
ThisBuild / startYear        := Some(2022)
ThisBuild / licenses         := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

lazy val root = project
  .in(file("."))
  .aggregate(zdata.js, zdata.jvm)
  .settings(
    crossScalaVersions := Nil,
    publish            := {},
    publishLocal       := {}
  )

lazy val zdata = crossProject(JSPlatform, JVMPlatform)
  .in(file("."))
  .settings(
    name := "zdata",
    scalacOptions ++= Seq("-feature", "-deprecation", "-Ywarn-unused"),
    semanticdbEnabled := true,
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio-streams"  % zioVersion % Compile,
      "dev.zio" %%% "zio-test"     % zioVersion % Test,
      "dev.zio" %%% "zio-test-sbt" % zioVersion % Test
    )
  )
  .jvmSettings(
    crossScalaVersions         := supportedScalaVersions,
    scalafixScalaBinaryVersion := "2.13",
    libraryDependencies ++= Seq(
    )
  )
  .jsSettings(
    scalafixScalaBinaryVersion      := "2.13",
    scalaJSUseMainModuleInitializer := true
  )
