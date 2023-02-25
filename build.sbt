import BuildHelper._
import explicitdeps.ExplicitDepsPlugin.autoImport.moduleFilterRemoveValue
import sbt.Keys

Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  List(
    organization     := "com.gregorpurdy",
    organizationName := "Gregor Purdy",
    startYear        := Some(2022),
    homepage         := Some(url("https://github.com/gnp/zdata")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "gnp",
        "Gregor Purdy",
        "gregor@abcelo.com",
        url("http://github.com/gnp")
      )
    )
  )
)

addCommandAlias("build", "; fmt; rootJVM/test")
addCommandAlias("fmt", "all root/scalafmtSbt root/scalafmtAll")
addCommandAlias("fmtCheck", "all root/scalafmtSbtCheck root/scalafmtCheckAll")
addCommandAlias(
  "check",
  "; scalafmtSbtCheck; scalafmtCheckAll"
)

addCommandAlias(
  "compileJVM",
  ";coreTestsJVM/test:compile;stacktracerJVM/test:compile;streamsTestsJVM/test:compile;testTestsJVM/test:compile;testMagnoliaTestsJVM/test:compile;testRefinedJVM/test:compile;testRunnerJVM/test:compile;examplesJVM/test:compile;macrosTestsJVM/test:compile;concurrentJVM/test:compile;managedTestsJVM/test:compile"
)
addCommandAlias(
  "testNative",
  ";coreTestsNative/test;stacktracerNative/test;streamsTestsNative/test;testTestsNative/test;examplesNative/Test/compile;macrosTestsNative/test;concurrentNative/test" // `test` currently executes only compilation, see `nativeSettings` in `BuildHelper`
)
addCommandAlias(
  "testJVM",
  ";coreTestsJVM/test;stacktracerJVM/test;streamsTestsJVM/test;testTestsJVM/test;testMagnoliaTestsJVM/test;testRefinedJVM/test;testRunnerJVM/test;testRunnerJVM/test:run;examplesJVM/test:compile;benchmarks/test:compile;macrosTestsJVM/test;testJunitRunnerTestsJVM/test;concurrentJVM/test;managedTestsJVM/test"
)
addCommandAlias(
  "testJVMNoBenchmarks",
  ";coreTestsJVM/test;stacktracerJVM/test;streamsTestsJVM/test;testTestsJVM/test;testMagnoliaTestsJVM/test;testRefinedJVM/test:compile;testRunnerJVM/test:run;examplesJVM/test:compile;concurrentJVM/test;managedTestsJVM/test"
)
addCommandAlias(
  "testJVM3",
  ";coreTestsJVM/test;stacktracerJVM/test:compile;streamsTestsJVM/test;testTestsJVM/test;testMagnoliaTestsJVM/test;testRefinedJVM/test;testRunnerJVM/test:run;examplesJVM/test:compile;concurrentJVM/test;managedTestsJVM/test"
)
addCommandAlias(
  "testJS3",
  ";coreTestsJS/test;stacktracerJS/test;streamsTestsJS/test;testTestsJS/test;testMagnoliaTestsJS/test;testRefinedJS/test;examplesJS/test:compile;concurrentJS/test"
)
addCommandAlias(
  "testJVM211",
  ";coreTestsJVM/test;stacktracerJVM/test;streamsTestsJVM/test;testTestsJVM/test;testRunnerJVM/test:run;examplesJVM/test:compile;macrosTestsJVM/test;concurrentJVM/test;managedTestsJVM/test"
)
addCommandAlias(
  "testJS",
  ";coreTestsJS/test;stacktracerJS/test;streamsTestsJS/test;testTestsJS/test;testMagnoliaTestsJS/test;testRefinedJS/test;examplesJS/test:compile;macrosTestsJS/test;concurrentJS/test"
)
addCommandAlias(
  "testJS211",
  ";coreTestsJS/test;stacktracerJS/test;streamsTestsJS/test;testTestsJS/test;examplesJS/test:compile;macrosJS/test;concurrentJS/test"
)

lazy val projectsCommon = List(
  streams,
  streamsTests
)

lazy val rootJVM = project.in(file("target/rootJVM")).settings(publish / skip := true).aggregate(rootJVM213)

lazy val rootJVM211 = project
  .in(file("target/rootJVM211"))
  .settings(publish / skip := true)
  .aggregate(projectsCommon.map(p => p.jvm: ProjectReference): _*)

lazy val rootJVM212 = project.in(file("target/rootJVM212")).settings(publish / skip := true).aggregate(rootJVM213)

lazy val rootJVM213 = project
  .in(file("target/rootJVM213"))
  .settings(publish / skip := true)
  .aggregate(projectsCommon.map(p => p.jvm: ProjectReference): _*)

lazy val rootJVM3 = project
  .in(file("target/rootJVM3"))
  .settings(publish / skip := true)
  .aggregate(projectsCommon.map(p => p.jvm: ProjectReference): _*)

lazy val rootJS = project
  .in(file("target/rootJS"))
  .settings(publish / skip := true)
  .aggregate(projectsCommon.map(p => p.js: ProjectReference): _*)

lazy val rootNative = project
  .in(file("target/rootNative"))
  .settings(publish / skip := true)
  .aggregate(projectsCommon.map(_.native: ProjectReference): _*)

lazy val root211 = project
  .in(file("target/root211"))
  .settings(publish / skip := true)
  .aggregate(
    (projectsCommon.flatMap(p => List[ProjectReference](p.jvm, p.js, p.native))): _*
  )

lazy val root212 = project.in(file("target/root212")).settings(publish / skip := true).aggregate(root213)

lazy val root213 = project
  .in(file("target/root213"))
  .settings(publish / skip := true)
  .aggregate(
    (projectsCommon.flatMap(p => List[ProjectReference](p.jvm, p.js, p.native))): _*
  )

lazy val root3 = project
  .in(file("target/root3"))
  .settings(publish / skip := true)
  .aggregate(
    (projectsCommon.flatMap(p => List[ProjectReference](p.jvm, p.js, p.native))): _*
  )

lazy val root = project
  .in(file("."))
  .settings(
    name           := "zio",
    publish / skip := true,
    unusedCompileDependenciesFilter -= moduleFilter(
      "org.scala-js",
      "scalajs-library"
    ),
    welcomeMessage
  )
  .aggregate(root213)
  .enablePlugins(ScalaJSPlugin)

lazy val streams = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .in(file("streams"))
  .settings(stdSettings("zio-streams"))
  .settings(crossProjectSettings)
  .settings(buildInfoSettings("zio.stream"))
  .settings(streamReplSettings)
  .enablePlugins(BuildInfoPlugin)
  .settings(macroDefinitionSettings)
  .settings(libraryDependencies += "dev.zio" %%% "zio-streams" % "2.0.9")
  .settings(
    scalacOptions ++= {
      if (scalaVersion.value == Scala3)
        Seq.empty
      else
        Seq("-P:silencer:globalFilters=[zio.stacktracer.TracingImplicits.disableAutoTrace]")
    }
  )
  .jsSettings(jsSettings)
  .nativeSettings(nativeSettings)

lazy val streamsTests = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .in(file("streams-tests"))
  .dependsOn(streams)
  .settings(libraryDependencies += "dev.zio" %%% "zio-test" % "2.0.9" % Test)
  .settings(libraryDependencies += "dev.zio" %%% "zio-test-sbt" % "2.0.9" % Test)
  .settings(stdSettings("streams-tests"))
  .settings(crossProjectSettings)
  .settings(testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"))
  .settings(buildInfoSettings("zio.stream"))
  .settings(publish / skip := true)
  .settings(
    Compile / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars
  )
  .enablePlugins(BuildInfoPlugin)
  .jsSettings(
    jsSettings,
    scalacOptions ++= {
      if (scalaVersion.value == Scala3) {
        List()
      } else {
        List("-P:scalajs:nowarnGlobalExecutionContext")
      }
    }
  )
  .nativeSettings(nativeSettings)

lazy val scalafixSettings = List(
  scalaVersion := Scala213,
  addCompilerPlugin(scalafixSemanticdb),
  crossScalaVersions --= List(Scala211, Scala212, Scala3),
  scalacOptions ++= List(
    "-Yrangepos",
    "-P:semanticdb:synthetics:on"
  )
)

lazy val scalafixRules = project.module
  .in(file("scalafix/rules")) // TODO .in needed when name matches?
  .settings(
    scalafixSettings,
    semanticdbEnabled                      := true, // enable SemanticDB
    libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % "0.10.4"
  )

/*


lazy val scala3                 = "3.2.2"
lazy val scala213               = "2.13.10"
lazy val scala212               = "2.12.17"
lazy val scala211               = "2.11.12"
lazy val supportedScalaVersions = List(scala3)

ThisBuild / scalaVersion := scala3
ThisBuild / version      := "0.1.0-SNAPSHOT"

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

addCommandAlias(
  "check",
  "; scalafmtSbtCheck; scalafmtCheckAll"
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
      "dev.zio" %%% "zio-streams"  % "2.0.9" % Compile,
      "dev.zio" %%% "zio-test"     % "2.0.9" % Test,
      "dev.zio" %%% "zio-test-sbt" % "2.0.9" % Test
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

 */
