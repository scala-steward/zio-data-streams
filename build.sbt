import Dependencies._
import sbt.Keys.libraryDependencies

lazy val scala3                 = "3.4.1"
lazy val scala213               = "2.13.11"
lazy val scala212               = "2.13.12"
lazy val scala211               = "2.11.12"
lazy val supportedScalaVersions = List(scala3)

ThisBuild / scalaVersion         := scala3
ThisBuild / version              := "0.1.1-SNAPSHOT"
ThisBuild / versionScheme        := Some("early-semver")
ThisBuild / organization         := "com.gregorpurdy"
ThisBuild / organizationName     := "Gregor Purdy"
ThisBuild / organizationHomepage := Some(url("https://github.com/gnp"))
ThisBuild / description          := "ZIO Streams integration with JDK DataInput and DataOutput format."
ThisBuild / startYear            := Some(2022)
ThisBuild / licenses             := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / homepage             := Some(url("https://github.com/gnp/zio-data-streams"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/gnp/zio-data-streams"),
    "scm:git@github.com:gnp/zio-data-streams.git"
  )
)
ThisBuild / developers := List(
  Developer(
    "gnp",
    "Gregor Purdy",
    "gregor@abcelo.com",
    url("http://github.com/gnp")
  )
)

// Remove all additional repository other than Maven Central from POM
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / pomIncludeRepository   := { _ => false }
ThisBuild / publishTo              := sonatypePublishToBundle.value

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

addCommandAlias(
  "check",
  "; headerCheck; scalafmtSbtCheck; scalafmtCheckAll; scalafixAll --check; doc"
)

addCommandAlias(
  "generateReadme",
  "; project docs; set mdocIn := file(\"docs/index.md\"); set mdocOut := file(\"README.md\"); doc / mdoc"
)

addCommandAlias(
  "testJVM3",
  ";test"
)
addCommandAlias(
  "testJS",
  ";test"
)

lazy val root = project
  .in(file("."))
  .aggregate(zioDataStreams.js, zioDataStreams.jvm)
  .settings(
    crossScalaVersions := Nil,
    publish            := {},
    publish / skip     := true,
    publishLocal       := {}
  )

lazy val docs = project
  .in(file("docs-build"))
  .settings(
    mdocVariables := Map(
      "VERSION" -> version.value.replaceAll("-SNAPSHOT$", "")
    )
  )
  .dependsOn(zioDataStreams.jvm)
  .enablePlugins(MdocPlugin)

lazy val zioDataStreams = crossProject(JSPlatform, JVMPlatform)
  .in(file("."))
  .settings(
    name := "zio-data-streams",
    scalacOptions ++= Seq("-feature", "-deprecation", "-Ywarn-unused"),
    semanticdbEnabled := true,
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio-streams"       % zioVersion % Compile,
      "dev.zio" %%% "zio-test"          % zioVersion % Test,
      "dev.zio" %%% "zio-test-sbt"      % zioVersion % Test,
      "dev.zio" %%% "zio-test-magnolia" % zioVersion % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
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
