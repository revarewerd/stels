package ru.sosgps.wayrecall.monitoring.notifications

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import org.springframework.context.ApplicationContext
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.monitoring.notifications.rules.NotificationRule

import scala.collection.immutable.List


class Rule(id: String) extends scala.annotation.StaticAnnotation

class Param(id: String, default: Any = null) extends scala.annotation.StaticAnnotation


class NotificationRuleLoader[T](classes: List[Class[_ <: NotificationRule[T]]]){
  import scala.reflect.runtime.universe._

  private val rm = runtimeMirror(this.getClass.getClassLoader)

  private val rulesMap = classes.flatMap(cl => {
    val classSymbol = rm.classSymbol(cl)
    getAnnotationValue[Rule](classSymbol, "id").map(rule => (rule, classSymbol))
  }).toMap


  private val notificationRule = rm.typeOf[NotificationRule[GPSData]]

  def isDefinedFor(rule: MongoDBObject) = rulesMap.contains(rule.as[String]("type"))

  def load(rule: MongoDBObject, applicationContext: ApplicationContext): NotificationRule[T] =
    load(NotificationRule.fromDbo[T](rule, applicationContext), rule)

  def load(base: => NotificationRule[T], rule: Imports.MongoDBObject): NotificationRule[T] = {
    val clazz = rulesMap(rule.as[String]("type"))
    val primaryConstructor = clazz.primaryConstructor
    val args: scala.List[Any] = primaryConstructor.asMethod.paramLists.head.map(s => {
      s.typeSignature match {
        case x if x =:= notificationRule => base
        case p =>
          val annotationValue = getAnnotationValue[Param](s, "id").get.asInstanceOf[String]
          val params = rule.as[MongoDBObject]("params")

          val paramValue = if(params.containsField(annotationValue))
            params.as[Any](annotationValue)
          else getAnnotationValue[Param](s, "default").get
          cast(rm.runtimeClass(p), paramValue)
      }
    })

    rm.reflectClass(clazz).reflectConstructor(primaryConstructor.asMethod).apply(args: _*)
      .asInstanceOf[NotificationRule[T]]
  }

  private def getAnnotationValue[A: TypeTag](s: Symbol, name: String): Option[Any] = {
    s.annotations.find(_.tree.tpe =:= rm.typeOf[A])
      .map(annotation => {
      val tree = annotation.tree
      val annotationArgs = tree.tpe.members.find(_.isConstructor).head.asMethod.paramLists.head
        .map(_.name.toString).zipWithIndex.toMap

      tree.children.tail(annotationArgs(name)).asInstanceOf[Literal].value.value.asInstanceOf[Any]
    })
  }

//  private def getAnnotationValue[A: TypeTag](s: Symbol): Option[String] = {
//    s.annotations.find(_.tree.tpe =:= rm.typeOf[A])
//      .map(annotation => annotation.tree.children.tail.head.asInstanceOf[Literal].value.value.asInstanceOf[String])
//  }

  private def cast(target: Class[_], value: Any):Any = {
    target match {
      //case java.lang.Byte.TYPE      => ClassTag.Byte.asInstanceOf[ClassTag[T]]
      //case java.lang.Short.TYPE     => ClassTag.Short.asInstanceOf[ClassTag[T]]
      //case java.lang.Character.TYPE => ClassTag.Char.asInstanceOf[ClassTag[T]]
      case java.lang.Integer.TYPE   => ru.sosgps.wayrecall.utils.tryInt(value)
      case java.lang.Long.TYPE      => ru.sosgps.wayrecall.utils.tryLong(value)
      case java.lang.Float.TYPE     => ru.sosgps.wayrecall.utils.tryDouble(value).toFloat
      case java.lang.Double.TYPE    => ru.sosgps.wayrecall.utils.tryDouble(value)
      case java.lang.Boolean.TYPE   => value.asInstanceOf[Boolean]
    }

  }

}