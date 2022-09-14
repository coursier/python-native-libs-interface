
import sbt._
import sbt.Keys._

import xerial.sbt.Sonatype.SonatypeKeys._

object Settings {

  def scala213 = "2.13.8"
  def scala212 = "2.12.16"

  lazy val shared = Seq(
    scalaVersion := scala213,
    crossScalaVersions := Seq(scala213, scala212),
    scalacOptions += "-target:jvm-1.8",
    javacOptions ++= Seq(
      "-source", "1.8",
      "-target", "1.8"
    ),
    Compile / doc / javacOptions := Seq("-source", "1.8"),
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
    sonatypeProfileName := "io.github.alexarchambault"
  )

  private val filterOut = Set("0.0.1")
  private def no212Versions = (0 to 14).map("0.0." + _).toSet

  // https://github.com/sbt/sbt-proguard/blob/2c502f961245a18677ef2af4220a39e7edf2f996/src/main/scala-sbt-1.0/com/typesafe/sbt/proguard/Sbt10Compat.scala#L8-L13
  // but sbt 1.4-compatible
  val getAllBinaryDeps: Def.Initialize[Task[Seq[java.io.File]]] = Def.task {
    import sbt.internal.inc.Analysis
    val converter = fileConverter.value
    (Compile / compile).value match {
      case analysis: Analysis =>
        analysis.relations.allLibraryDeps.toSeq.map(converter.toPath(_).toFile)
    }
  }

  lazy val rtJarOpt = sys.props.get("sun.boot.class.path")
    .toSeq
    .flatMap(_.split(java.io.File.pathSeparator).toSeq)
    .map(java.nio.file.Paths.get(_))
    .find(_.endsWith("rt.jar"))
    .map(_.toFile)

}
