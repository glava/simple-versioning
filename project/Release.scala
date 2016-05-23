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

  val checkOrganization = ReleaseStep(action = st => {
    // extract the build state
    import sbt._
    val extracted = Project.extract(st)

    val file: File = new File(extracted.currentProject.base.absolutePath + "/version.sbt")
    st.log.info(file.absolutePath)
    st
  })

  lazy val customReleaseSteps = Seq[ReleaseStep](
    inquireVersions,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    setNextVersion,
    commitNextVersion,
    pushChanges
  )


  def assemblyVersion(version: String, headCommit: Option[String]) = {
    headCommit match {
      case Some(hash) if version.endsWith("-SNAPSHOT") => s"$version-${hash.take(6)}"
      case _ => version
    }
  }
}
