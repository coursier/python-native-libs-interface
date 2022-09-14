package io.github.alexarchambault.pythonnativelibs.internal

import ai.kien.python.Python

import java.util.{Map => JMap}

import scala.jdk.CollectionConverters._

object PythonNativeLibsHelper {

  private lazy val python = Python()

  def ldflags: Array[String] =
    python.ldflags.get.toArray
  def executable: String =
    python.executable.get
  def ldflagsNix: Array[String] =
    python.ldflagsNix.get.toArray
  def ldflagsWin: Array[String] =
    python.ldflagsWin.get.toArray
  def nativeLibrary: String =
    python.nativeLibrary.get
  def nativeLibraryPaths: Array[String] =
    python.nativeLibraryPaths.get.toArray
  def scalapyProperties: JMap[String, String] =
    python.scalapyProperties.get.asJava

}