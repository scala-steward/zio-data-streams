addSbtPlugin("org.scalameta"      % "sbt-scalafmt"                  % "2.4.6")
addSbtPlugin("de.heikoseeberger"  % "sbt-header"                    % "5.7.0")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"                   % "1.12.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"      % "1.0.0")
addSbtPlugin("org.scala-native"   % "sbt-scala-native"              % "0.4.9")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.2.0")
addSbtPlugin("ch.epfl.scala"      % "sbt-scalafix"                  % "0.10.4")
addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo"                 % "0.11.0")
addSbtPlugin("com.github.cb372"   % "sbt-explicit-dependencies"     % "0.2.16")

libraryDependencies += "org.snakeyaml" % "snakeyaml-engine" % "2.3"
