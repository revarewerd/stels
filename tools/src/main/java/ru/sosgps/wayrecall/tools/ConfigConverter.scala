package ru.sosgps.wayrecall.tools

import java.io.{File, FileInputStream}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file._
import java.util.Properties

import com.google.common.base.Charsets

import scala.collection.JavaConverters._
import scala.collection.immutable.TreeSet

// scp -r niks@wayrecall.ksb-stels.ru:/home/niks/Wayrecallhome/conf/wrcinstances ./b4-conf

object ConfigConverter {

  def main(args: Array[String]): Unit = {

    import CCUtils._

    val wrcInstancesDir = new File(args(0))

    val instancesInfo = wrcInstancesDir.listFiles().map(instanceDir => {
      InstanceInfo(
        instanceDir.getName,
        BillingAdminEntry.admins(instanceDir),
        CCUtils.loadProperties(instanceDir, "instance.properties").asScala.toMap,
        com.google.common.io.Files.readLines(new File(instanceDir, "vhosts"), Charsets.UTF_8).asScala.toList
      )
    }).toList

    def renderAdmins(entries: Iterable[BillingAdminEntry]) = {
      def record(admin: BillingAdminEntry) = s"{name: ${admin.name.wrapDQ()}, password: ${admin.password.wrapDQ()}}"

      if (entries.size < 2)
        entries.map(record).mkString(" [", ", ", "]")
      else
        (for (admin <- entries) yield {
          s"\n|      - ${record(admin)}"
        }).mkString("")
    }

    println("wrcinstances_data:")
    println((for (instance <- instancesInfo) yield {
      s"""  - name: ${instance.name.wrapDQ()}
         |    hosts: [${instance.hosts.map(_.wrapDQ()).mkString(", ")}]${
        mapProps("|    ", instance.data,
          "homesite" -> "instance.homesite",
          "parent" -> "instance.parent",
          "m2mSmsGate" -> "instance.m2mSmsGate.enabled")
      }
         |    mongo:
         |      name: ${instance.data("instance.defaultmongodb.databaseName").wrapDQ()}
         |    admins:${
        renderAdmins(instance.adminEntries)
      }""".stripMargin
    }).mkString("\n"))

    val managedFiles = Set("instance.properties", "billingAdmins.properties", "vhosts")
    val extractedAddidionalDataDir = wrcInstancesDir.getParentFile.toPath.resolve("wrc-instances-additional-files")
    if (!Files.exists(extractedAddidionalDataDir))
      Files.createDirectory(extractedAddidionalDataDir)

    val wrcPath = wrcInstancesDir.toPath
    Files.walkFileTree(wrcPath, new SimpleFileVisitor[Path] {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        val relative = wrcPath.relativize(file)
        if(!managedFiles(relative.subpath(1, relative.getNameCount ).toString)){
          val newPath = extractedAddidionalDataDir.resolve(relative)
          Files.createDirectories(newPath.getParent)
          Files.copy(file, newPath, StandardCopyOption.REPLACE_EXISTING)
        }
        super.visitFile(file, attrs)
      }
    })

  }

}

case class InstanceInfo(name: String, adminEntries: List[BillingAdminEntry], data: Map[String, String], hosts: List[String])

case class BillingAdminEntry(name: String, password: String, roles: TreeSet[String], enabled: String)

object BillingAdminEntry {

  def apply(name: String, line: String): BillingAdminEntry = {
    val vector = line.split(",").toVector

    new BillingAdminEntry(name, vector(0), vector.slice(1, vector.length - 1).to[TreeSet], vector.last)
  }

  def admins(instanceDir: File) = {
    val properties = CCUtils.loadProperties(instanceDir, "billingAdmins.properties")
    properties.keys().asScala.map(key => BillingAdminEntry(key.asInstanceOf[String], properties.get(key).asInstanceOf[String])).toList
  }

}

object CCUtils {
  def loadProperties(instanceDir: File, fileName: String) = {
    val properties = new Properties()
    properties.load(new FileInputStream(new File(instanceDir, fileName)))
    properties
  }

  def mapProps(prefix: String, data: Map[String, String], km: (String, String)*): String = {
    (for ((n, o) <- km;
          value <- data.get(o)) yield {
      s"\n$prefix$n: ${value.wrapDQ()}"
    }).mkString(s"")
  }

  implicit class StringOps(val str: String) {
    def wrapDQ() = s""""${str}""""
  }

}
