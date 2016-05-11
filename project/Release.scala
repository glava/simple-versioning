import sbt.Project
import sbtrelease.Git
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._

object Release {
  lazy val mergeDevelop = ReleaseStep(action = st => {
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

  lazy val masterOnly = ReleaseStep(action = st => {
    val extracted = Project.extract(st)

    val git = extracted.get(releaseVcs).get.asInstanceOf[Git]
    val curBranch = (git.cmd("rev-parse", "--abbrev-ref", "HEAD") !!).trim
    if (curBranch != "master") throw new IllegalArgumentException("Releases are available from master branch")
    st
  })

  lazy val customReleaseSteps = Seq[ReleaseStep](
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

  val VersionRegex = "([0-9]+.[0-9]+.[0-9]+)-?(.*)?".r
  def assemblyVersion(version: String, gitDescription: Option[String]) = {
    gitDescription match {
      case Some(VersionRegex(v, ""))              => version
      case Some(VersionRegex(v, s)) if !s.isEmpty => s"${version.replace("-SNAPSHOT", "")}-$s-SNAPSHOT"
      case None                                   => version
    }
  }
}
