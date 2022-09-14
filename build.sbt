import scala.xml.{Node => XmlNode, _}
import scala.xml.transform.{RewriteRule, RuleTransformer}

inThisBuild(List(
  organization := "io.github.alexarchambault.python",
  homepage := Some(url("https://github.com/alexarchambault/python-native-libs-interface")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      "alexarchambault",
      "Alexandre Archambault",
      "alexandre.archambault@gmail.com",
      url("https://github.com/alexarchambault")
    )
  ),
  sonatypeCredentialHost := "s01.oss.sonatype.org",
  sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
  sonatypeProfileName := "io.github.alexarchambault"
))

lazy val finalPackageBin = taskKey[File]("")

def updateIvyXml(file: File): Unit = {
  val content = scala.xml.XML.loadFile(file)
  val updatedContent = content.copy(child = content.child.map {
    case elem: Elem if elem.label == "dependencies" =>
      elem.copy(child = elem.child.map {
        case elem: Elem if elem.label == "dependency" =>
          (elem.attributes.get("org"), elem.attributes.get("name")) match {
            case (Some(Seq(orgNode)), Some(Seq(nameNode))) =>
              val org = orgNode.text
              val name = nameNode.text
              Comment(
                s""" shaded dependency $org:$name
                   | $elem
                   |""".stripMargin
              )
            case _ => elem
          }
        case n => n
      })
    case n => n
  })
  val printer = new scala.xml.PrettyPrinter(Int.MaxValue, 2)
  val updatedFileContent = """<?xml version="1.0" encoding="UTF-8"?>""" + '\n' +
    "<!-- hello -->\n" +
    printer.format(updatedContent)
  java.nio.file.Files.write(file.toPath, updatedFileContent.getBytes("UTF-8"))
}

lazy val reallyUpdateIvyXml = Def.task {
  val baseFile = deliverLocal.value
  val log = streams.value.log
  val resolverName = publishLocalConfiguration.value.resolverName.getOrElse(???)
  ivyModule.value.withModule(log) {
    case (ivy, md, _) =>
      val resolver = ivy.getSettings.getResolver(resolverName)
      val artifact = new org.apache.ivy.core.module.descriptor.MDArtifact(md, "ivy", "ivy", "xml", true)
      log.info(s"Writing ivy.xml with shading at $baseFile")
      resolver.publish(artifact, baseFile, true)
  }
}

lazy val interface = project
  .enablePlugins(SbtProguard)
  .settings(
    moduleName := {
      val former = moduleName.value
      val sv = scalaVersion.value
      val sbv = sv.split('.').take(2).mkString(".")
      if (sv == Settings.scala213)
        former
      else
        former + "-scala-" + sbv + "-shaded"
    },
    finalPackageBin := {
      import com.eed3si9n.jarjar._
      import com.eed3si9n.jarjar.util.StandaloneJarProcessor

      val orig = (Proguard / proguard).value.head
      val origLastModified = orig.lastModified()
      val dest = orig.getParentFile / s"${orig.getName.stripSuffix(".jar")}-with-renaming-test.jar"
      if (!dest.exists() || dest.lastModified() < origLastModified) {
        val tmpDest = orig.getParentFile / s"${orig.getName.stripSuffix(".jar")}-with-renaming-test-0.jar"

        def rename(from: String, to: String): Rule = {
          val rule = new Rule
          rule.setPattern(from)
          rule.setResult(to)
          rule
        }

        val rules = Seq(
          rename("scala.**", "io.github.alexarchambault.pythonnativelibs.shaded.scala.@1"),
          rename("ai.kien.python.**", "io.github.alexarchambault.pythonnativelibs.shaded.python.@1"),
          // rename("org.fusesource.**", "io.github.alexarchambault.pythonnativelibs.shaded.org.fusesource.@1"),
          // rename("io.github.alexarchambault.windowsansi.**", "io.github.alexarchambault.pythonnativelibs.shaded.windowsansi.@1"),
          // rename("concurrentrefhashmap.**", "io.github.alexarchambault.pythonnativelibs.shaded.concurrentrefhashmap.@1"),
          // rename("org.apache.commons.compress.**", "io.github.alexarchambault.pythonnativelibs.shaded.commonscompress.@1"),
          // rename("org.apache.commons.io.input.**", "io.github.alexarchambault.pythonnativelibs.shaded.commonsio.@1"),
          // rename("org.codehaus.plexus.**", "io.github.alexarchambault.pythonnativelibs.shaded.plexus.@1"),
          // rename("org.tukaani.xz.**", "io.github.alexarchambault.pythonnativelibs.shaded.xz.@1"),
          // rename("org.iq80.snappy.**", "io.github.alexarchambault.pythonnativelibs.shaded.snappy.@1"),
          // rename("com.github.plokhotnyuk.jsoniter_scala.core.**", "io.github.alexarchambault.pythonnativelibs.shaded.jsoniter.@1")
        )

        val processor = new com.eed3si9n.jarjar.JJProcessor(
          rules,
          verbose = false,
          skipManifest = true,
          misplacedClassStrategy = "fatal"
        )
        StandaloneJarProcessor.run(orig, tmpDest, processor)

        val toBeRemoved = Set(
          "LICENSE",
          "NOTICE",
          "README",
          "scala-collection-compat.properties"
        )
        val directoriesToBeRemoved = Seq(
          "licenses/"
        )
        assert(directoriesToBeRemoved.forall(_.endsWith("/")))
        ZipUtil.removeFromZip(
          tmpDest,
          dest,
          name =>
            toBeRemoved(name) || directoriesToBeRemoved.exists(dir =>
              name.startsWith(dir)
            )
        )
        tmpDest.delete()
      }
      Check.onlyNamespace("io/github/alexarchambault/pythonnativelibs", dest)
      dest
    },
    addArtifact(Compile / packageBin / artifact, finalPackageBin),
    Proguard / proguardVersion := "7.2.2",
    Proguard / proguardOptions ++= {
      val baseOptions = Seq(
        "-dontnote",
        "-dontwarn",
        "-dontobfuscate",
        "-dontoptimize",
        "-keep class io.github.alexarchambault.pythonnativelibs.** {\n  public protected *;\n}"
      )

      val isJava9OrMore = sys.props.get("java.version").exists(!_.startsWith("1."))
      val maybeJava9Options =
        if (isJava9OrMore) {
          val javaHome = sys.props.getOrElse("java.home", ???)
          Seq(s"-libraryjars $javaHome/jmods/java.base.jmod")
        }
        else
          Nil

      baseOptions ++ maybeJava9Options
    },
    Proguard / proguard / javaOptions := Seq("-Xmx3172M"),

    // Adding the interface JAR rather than its classes directory.
    // The former contains META-INF stuff in particular.
    Proguard / proguardInputs := (Proguard / proguardInputs).value.filter(f => !f.isDirectory || f.getName != "classes"),
    Proguard / proguardInputs += (Compile / packageBin).value,

    Proguard / proguardBinaryDeps := Settings.getAllBinaryDeps.value,
    Proguard / proguardBinaryDeps ++= Settings.rtJarOpt.toSeq, // seems needed with sbt 1.4.0

    Proguard / proguardInputFilter := { file =>
      file.name match {
        case n if n.startsWith("interface") => None // keep META-INF from main JAR
        case n if n.startsWith("scala-xml") => Some("!META-INF/**,!scala-xml.properties")
        case n if n.startsWith("scala-library") => Some("!META-INF/**,!library.properties,!rootdoc.txt")
        case _ => Some("!META-INF/**")
      }
    },

    publishLocal := {
      reallyUpdateIvyXml.dependsOn(publishLocal).value
    },

    deliverLocal := {
      val file = deliverLocal.value
      updateIvyXml(file)
      file
    },

    // inspired by https://github.com/olafurpg/coursier-small/blob/408528d10cea1694c536f55ba1b023e55af3e0b2/build.sbt#L44-L56
    pomPostProcess := { node =>
      new RuleTransformer(new RewriteRule {
        override def transform(node: XmlNode) = node match {
          case _: Elem if node.label == "dependency" =>
            val org = node.child.find(_.label == "groupId").fold("")(_.text.trim)
            val name = node.child.find(_.label == "artifactId").fold("")(_.text.trim)
            val ver = node.child.find(_.label == "version").fold("")(_.text.trim)
            Comment(s"shaded dependency $org:$name:$ver")
          case _ => node
        }
      }).transform(node).head
    },

    Settings.shared,
    libraryDependencies ++= Seq(
      "ai.kien" %% "python-native-libs" % "0.2.4"
    ),

    libraryDependencies += "com.lihaoyi" %% "utest" % "0.8.0" % Test,
    testFrameworks += new TestFramework("utest.runner.Framework"),

    // clearing scalaModuleInfo in ivyModule, so that evicted doesn't
    // check scala versions
    ivyModule := {
      val is = ivySbt.value
      val config = moduleSettings.value match {
        case config0: ModuleDescriptorConfiguration =>
          config0.withScalaModuleInfo(None)
	      case other => other
      }
      new is.Module(config)
    },
    autoScalaLibrary := false,
    crossVersion := CrossVersion.disabled
  )

publish / skip := true
Settings.shared
