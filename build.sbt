import sbt._

name := "simple-versioning"

scalaVersion := "2.11.8"

def projectTemplate(projectName: String): Project = Project(projectName, file(projectName))
  .enablePlugins(GitVersioning)
  .settings(
    scalaVersion := "2.11.8",
    outputStrategy := Some(StdoutOutput),
    test in assembly := {},
    git.useGitDescribe := true,
    publishTo := None,
    git.baseVersion := "0.0.0",
    assemblyJarName in assembly := s"$projectName-${Release.assemblyVersion(version.value, git.gitDescribedVersion.value)}.jar",
    releaseProcess := Release.customReleaseSteps
  )

releaseProcess := Release.customReleaseSteps

lazy val scheduler = projectTemplate("scheduler")

lazy val worker = projectTemplate("worker")
