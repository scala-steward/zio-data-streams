sonatypeProfileName := "com.gregorpurdy"

publishMavenStyle := true

licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

import xerial.sbt.Sonatype._
sonatypeProjectHosting := Some(GitHubHosting("gnp", "zio-data-streams", "gregor@abcelo.com"))
