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

val mergeDevelop = ReleaseStep(action = st => {
  // extract the build state
  val extracted = Project.extract(st)

  val git = extracted.get(releaseVcs).get.asInstanceOf[Git]
  val curBranch = (git.cmd("rev-parse", "--abbrev-ref", "HEAD") !!).trim
  git.cmd("checkout", "develop") ! st.log
  git.cmd("pull", "origin", "develop") ! st.log
  git.cmd("merge", "master") ! st.log
  git.cmd("push", "origin", "master") ! st.log
  git.cmd("checkout", "master") ! st.log
  st.log.info("Develop merged with master")
  st
})

val masterOnly = ReleaseStep(action = st => {
  val extracted = Project.extract(st)

  val git = extracted.get(releaseVcs).get.asInstanceOf[Git]
  val curBranch = (git.cmd("rev-parse", "--abbrev-ref", "HEAD") !!).trim
  st.log.info(curBranch)
  st
})

def projectTemplate(projectName: String): Project = Project(projectName, file(projectName))
  .enablePlugins(GitVersioning)
  .settings(
    scalaVersion := "2.11.8",
    outputStrategy := Some(StdoutOutput),
    test in assembly := {},
    git.useGitDescribe := true,
    publishTo := None,
    git.baseVersion := "0.0.0",
    assemblyJarName in assembly := s"$projectName-${gitVersionConversion(git.gitDescribedVersion.value)}.jar",
    releaseProcess :=  Seq[ReleaseStep](
      masterOnly,
      checkSnapshotDependencies,
      inquireVersions,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion,
      pushChanges,
      mergeDevelop
    )
)

lazy val scheduler = projectTemplate("scheduler")

lazy val worker = projectTemplate("worker")
