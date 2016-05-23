import sbt.Project
import sbtrelease.Git
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import sbtassembly.AssemblyKeys._
object Release {

  lazy val mergeDevelop = ReleaseStep(action = st => {
    val extracted = Project.extract(st)

    val git = extracted.get(releaseVcs).get.asInstanceOf[Git]
    git.cmd("checkout", "develop") ! st.log
    git.cmd("pull", "origin", "develop") ! st.log
    git.cmd("merge", "master") ! st.log
    git.cmd("push", "origin", "develop") ! st.log
    git.cmd("checkout", "master") ! st.log
    st.log.info("Develop merged with master")
    st
  })

  lazy val assembly2 = ReleaseStep(action = st => {
    val extracted = Project.extract(st)
    val (newState, env) = extracted.runTask(assembly, st)
    newState
  })

  lazy val customReleaseSteps = Seq[ReleaseStep](
    inquireVersions,
    setReleaseVersion,
    commitReleaseVersion,
    assembly2,
    tagRelease,
    setNextVersion,
    commitNextVersion,
    pushChanges,
    mergeDevelop
  )

  def assemblyVersion(version: String, headCommit: Option[String]) = {
    headCommit match {
      case Some(hash) if version.endsWith("-SNAPSHOT") => s"$version-$hash"
      case _ => version
    }
  }
}
