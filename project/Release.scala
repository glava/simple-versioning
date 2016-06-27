import sbt.Keys._
import sbt.{Cross, Extracted, Load, Project, State}
import sbtrelease.Git
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import sbtassembly.AssemblyKeys._
object Release {
  val HashSize: Int = 6

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


  lazy val crossAssembly: ReleaseStep = runCrossBuild(assembleJar.action)

  lazy val assembleJar: ReleaseStep = ReleaseStep(action = st => {
    val extracted = Project.extract(st)
    val (newState, _) = extracted.runTask(assembly, st)
    newState
  })

  lazy val prepareApplicationConf: ReleaseStep = ReleaseStep(action = st => {
    sbt.IO.copyFile(
      new java.io.File("worker/src/main/resources/production.conf"),
      new java.io.File("worker/src/main/resources/application.conf")
    )

    st
  })

  lazy val revertingApplicationConf: ReleaseStep = ReleaseStep(action = st => {
    val extracted = Project.extract(st)

    val git = extracted.get(releaseVcs).get.asInstanceOf[Git]
    git.cmd("checkout", "worker/src/main/resources") ! st.log
    st
  })

  private def switchScalaVersion(state: State, version: String): State = {
    val x = Project.extract(state)
    import x._
    state.log.info("Setting scala version to " + version)
    val add = (scalaVersion in sbt.GlobalScope := version) :: (scalaHome in sbt.GlobalScope := None) :: Nil
    val cleared = session.mergeSettings.filterNot(Cross.crossExclude)
    val newStructure = Load.reapply(add ++ cleared, structure)
    Project.setProject(session, newStructure, state)
  }

  private def runCrossBuild(func: State => State): State => State = { state =>
    val x = Project.extract(state)
    import x._
    val versions = Cross.crossVersions(state)
    val current = scalaVersion in currentRef get structure.data
    val finalS = (state /: versions) {
      case (s, v) => func(switchScalaVersion(s, v))
    }
    current.map(switchScalaVersion(finalS, _)).getOrElse(finalS)
  }

  lazy val customReleaseSteps = Seq[ReleaseStep](
    checkSnapshotDependencies,
    runClean,
    runTest,
    inquireVersions,
    setReleaseVersion,
    commitReleaseVersion,
    prepareApplicationConf,
    crossAssembly,
    revertingApplicationConf,
    tagRelease,
    setNextVersion,
    commitNextVersion,
    pushChanges,
    mergeDevelop
  )

  def assemblyVersion(version: String, headCommit: Option[String]) =
    headCommit match {
      case Some(hash) if version.endsWith("-SNAPSHOT")  => s"$version-${hash.take(HashSize)}"
      case _                                            => version
    }
}
