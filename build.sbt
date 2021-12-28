import Settings._
import xerial.sbt.Sonatype._

val versionV = "0.0.6"

ThisBuild / version      := versionV
ThisBuild / scalaVersion := Versions.dotty

ThisBuild / githubWorkflowTargetTags           ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / githubWorkflowPublish               := Seq(WorkflowStep.Sbt(List("release")))
ThisBuild / githubWorkflowJavaVersions          := Seq("adopt@1.11")
ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(List("compile"))
)
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("release"),
    env = Map(
      "PGP_PASSPHRASE"    -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET"        -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)
ThisBuild / credentials += Credentials("Sonatype Nexus Repository Manager",
                                       "oss.sonatype.org",
                                       sys.env.getOrElse("SONATYPE_USERNAME", ""),
                                       sys.env.getOrElse("SONATYPE_PASSWORD", ""))

ThisBuild / scmInfo := Some(
  ScmInfo(url("https://github.com/vlmiroshnikov/aero3"), "git@github.com:vlmiroshnikov/aero3.git")
)
ThisBuild / developers ++= List(
  "vlmiroshnikov" -> "Vyacheslav Miroshnikov"
).map {
  case (username, fullName) =>
    Developer(username, fullName, s"@$username", url(s"https://github.com/$username"))
}

ThisBuild / organization     := "io.github.vlmiroshnikov"
ThisBuild / organizationName := "vlmiroshnikov"

lazy val release = taskKey[Unit]("Release")
addCommandAlias("release", "; reload; project /; publishSigned; sonatypeBundleRelease")

def publishSettings = Seq(
  sonatypeProfileName := "io.github.vlmiroshnikov",
  publishMavenStyle   := true,
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0")),
  sonatypeProjectHosting := Some(
    GitHubHosting("vlmiroshnikov", "aero3", "vlmiroshnikov@gmail.com")),
  homepage       := Some(url("https://github.com/vlmiroshnikov/aero3")),
  publishTo      := sonatypePublishToBundle.value,
  useGpgPinentry := Option(System.getenv("PGP_PASSPHRASE")).isDefined
)

lazy val aero = project
  .in(file("."))
  .settings(scalaVersion := Versions.dotty)
  .aggregate(`aero-core`, example)
  .settings(
    publish         := {},
    publishLocal    := {},
    publishArtifact := false,
    publish / skip  := true
  )

lazy val `aero-core` = project
  .in(file("aero-core"))
  .settings(
    name                 := "aero-core",
    scalaVersion         := Versions.dotty,
    libraryDependencies ++= cats ++ catsEffect ++ munit ++ munitEffect ++ aerospike
  )
  .settings(publishSettings)

lazy val example = project
  .in(file("example"))
  .dependsOn(`aero-core`)
  .settings(
    name                 := "example",
    libraryDependencies ++= munit ++ munitEffect ++ catsEffect ++ cats
  )
  .settings(
    publish         := {},
    publishLocal    := {},
    publishArtifact := false,
    publish / skip  := true
  )
