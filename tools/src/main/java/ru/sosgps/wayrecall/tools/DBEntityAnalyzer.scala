package ru.sosgps.wayrecall.tools

import java.util.Properties

import com.beust.jcommander.{Parameter, Parameters}
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.core.env.{MutablePropertySources, PropertiesPropertySource, StandardEnvironment}
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.jcommanderutils.CliCommand
import com.mongodb.casbah.Imports._

import scala.collection.mutable

object Tabulator {
  def format(table: Seq[Seq[Any]]) = table match {
    case Seq() => ""
    case _ =>
      val sizes = for (row <- table) yield for (cell <- row) yield if (cell == null) 0 else cell.toString.length
      val colSizes = for (col <- sizes.transpose) yield col.max
      val rows = for (row <- table) yield formatRow(row, colSizes)
      formatRows(rowSeparator(colSizes), rows)
  }

  def formatRows(rowSeparator: String, rows: Seq[String]): String = (
    rowSeparator ::
      rows.head ::
      rowSeparator ::
      rows.tail.toList :::
      rowSeparator ::
      List()).mkString("\n")

  def formatRow(row: Seq[Any], colSizes: Seq[Int]) = {
    val cells = for ((item, size) <- row.zip(colSizes)) yield if (size == 0) "" else ("%" + size + "s").format(item)
    cells.mkString("|", "|", "|")
  }

  def rowSeparator(colSizes: Seq[Int]) = colSizes map { "-" * _ } mkString("+", "+", "+")
}

@Parameters(commandDescription = "Analyzes field usage stats")
object DBEntityAnalyzer extends CliCommand {
  var mdbm: MongoDBManager = null
  var context: ClassPathXmlApplicationContext = null
  override val commandName: String = "entityAnalyzer"

  @Parameter(names = Array("-d", "-db"), required = false, description = "database name, default loaded from config")
  var dbname: String = null

  @Parameter(names = Array("-c"), required = true, description = "Collection name")
  var collName: String = null

  @Parameter(names = Array("-f"), required = false, description = "Field name")
  var fieldName: String = null

  override def process(): Unit = {
    context = new ClassPathXmlApplicationContext();
    context.setConfigLocation("axon-config.xml"); // TODO упростить
    if (dbname != null) {
      context.setEnvironment(new StandardEnvironment {
        protected override def customizePropertySources(propertySources: MutablePropertySources) {
          super.customizePropertySources(propertySources)
          val generatedProps: Properties = new Properties
          generatedProps.setProperty("instance.defaultmongodb.databaseName", dbname)
          propertySources.addFirst(new PropertiesPropertySource("generated", generatedProps))
        }
      })
    }

    context.refresh();
    context.registerShutdownHook();
    mdbm = context.getBean(classOf[MongoDBManager])

    if(fieldName != null)
      printObjectsWithoutField()
    else
      printSummary()
  }

  case class FieldStats(var freq: Int, var nullCount: Int, var emptyStringCount: Int, var emptyListCount: Int, var types: mutable.Map[String, Int] = mutable.Map())
  def printSummary(): Unit = {
    val collection = mdbm.getDatabase()(collName)

    val stats = mutable.Map[String, FieldStats]()

    val collSize =  collection.size

    collection.foreach(dbo => {
      dbo.keys.foreach(k => {
        val fieldStats = stats.getOrElse(k, FieldStats(0,0,0,0))
        fieldStats.freq += 1
        if(dbo.get(k) == null)
          fieldStats.nullCount += 1
        else {
          val fieldValue = dbo.get(k)
          val fieldClassName = fieldValue.getClass.getSimpleName
          fieldStats.types(fieldClassName) = fieldStats.types.getOrElse(fieldClassName, 0) + 1
          if(dbo.getAs[String](k).exists(_.isEmpty))
            fieldStats.emptyStringCount += 1
          if(dbo.getAs[BasicDBList](k).exists(_.isEmpty))
            fieldStats.emptyListCount += 1
        }

        stats += (k -> fieldStats)
      })
    })

    println("size = " + stats.size)

    val rows = stats.toList.sortBy{case(k, v) => k}.map{
      case(name, data) => List(name)++ List(data.freq, data.nullCount, data.emptyStringCount, data.emptyListCount).map(_ + s"/${collSize}") :+  data.types.map{case(t,c) => s"$t($c)"}.mkString(", ")
    }
    println(Tabulator.format(List("Field", "Exists", "Null", "\"\"", "[]", "Type") +: rows))
  }

  def printObjectsWithoutField(): Unit = {
    val collection = mdbm.getDatabase()(collName)
    val size =  collection.size
    val objectsWithoutField = mutable.ArrayBuffer[DBObject]()


    collection.foreach(dbo => {
      if(!dbo.containsField(fieldName)) {
        objectsWithoutField.append(dbo)
      }
    })

    println("ObjectWithoutField:\n")
    objectsWithoutField.foreach(println)
    println()

  }
}
