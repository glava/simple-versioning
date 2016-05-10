import sbtrelease._
import sbtrelease.ReleaseStateTransformations._

name := "simple-versioning"

scalaVersion := "2.11.8"

val VersionRegex = "([0-9]+.[0-9]+.[0-9]+)-?(.*)?".r

def gitVersionConversion(version: Option[String]) = {
  println(version)
  version match {
    case Some(VersionRegex(v, "SNAPSHOT")) => s"$v-SNAPSHOT"
    case Some(VersionRegex(v, "")) => v
    case Some(VersionRegex(v, s)) => s"$v-$s-SNAPSHOT"
    case v => ""
  }
}

def projectTemplate(projectName: String): Project = Project(projectName, file(projectName))
  .enablePlugins(GitVersioning)
  .settings(
    scalaVersion := "2.11.8",
    outputStrategy := Some(StdoutOutput),
    test in assembly := {},
    git.useGitDescribe := true,
    git.baseVersion := "0.0.0",
    assemblyJarName in assembly := s"$projectName${gitVersionConversion(git.gitDescribedVersion.value)}.jar"
)

lazy val scheduler = projectTemplate("scheduler")

lazy val worker = projectTemplate("worker")

releaseProcess := Seq(
  checkSnapshotDependencies,
  inquireVersions,
  setReleaseVersion,
  tagRelease
)
