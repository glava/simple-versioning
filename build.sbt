import sbt._
import sbtrelease._
import sbtrelease.ReleaseStateTransformations._

name := "simple-versioning"

scalaVersion := "2.11.8"

val VersionRegex = "([0-9]+.[0-9]+.[0-9]+)-?(.*)?".r

def gitVersionConversion(version: Option[String]) = {
  println(version)
  version match {
    case Some(VersionRegex(v, ""))          => v
    case Some(VersionRegex(v, s))           => s"$v-$s-SNAPSHOT"
    case None                               => s"-SNAPSHOT"
  }
}

def projectTemplate(projectName: String): Project = Project(projectName, file(projectName))
  .enablePlugins(GitVersioning)
  .settings(
    scalaVersion := "2.11.8",
    outputStrategy := Some(StdoutOutput),
    test in assembly := {},
    git.useGitDescribe := true,
    publishTo := None,
    git.baseVersion := "0.0.0",
    assemblyJarName in assembly := s"$projectName${gitVersionConversion(git.gitDescribedVersion.value)}.jar",
    releaseProcess :=  Seq[ReleaseStep](
      checkSnapshotDependencies,              // : ReleaseStep
      inquireVersions,                        // : ReleaseStep
      runTest,                                // : ReleaseStep
      setReleaseVersion,                      // : ReleaseStep
      commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
      tagRelease,                             // : ReleaseStep
      setNextVersion,                         // : ReleaseStep
      commitNextVersion,                      // : ReleaseStep
      pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
    )

)


lazy val scheduler = projectTemplate("scheduler")

lazy val worker = projectTemplate("worker")
