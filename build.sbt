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
  // retrieve the value of the organization SettingKey
  val org = extracted.get(Keys.organization)
  val git = extracted.get(releaseVcs).get.asInstanceOf[Git]
  if (org.startsWith("com.acme"))
    sys.error("Hey, no need to release a toy project!")

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
      /*checkSnapshotDependencies,
      inquireVersions,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion,
      pushChanges,*/
      mergeDevelop
    )
)

lazy val scheduler = projectTemplate("scheduler")

lazy val worker = projectTemplate("worker")
