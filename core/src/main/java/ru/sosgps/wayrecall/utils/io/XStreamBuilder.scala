package ru.sosgps.wayrecall.utils.io

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.mapper.MapperWrapper

/**
 * Created by nickl on 10.03.15.
 */
object XStreamBuilder extends grizzled.slf4j.Logging{

  def getXStream = {
    new XStream(){
      override def wrapMapper(next: MapperWrapper):MapperWrapper = new MapperWrapper(next) {


        override def realClass(elementName: String): Class[_] = {
          val clazz = elementName match {
            case "scala.collection.immutable.$colon$colon" => super.realClass("scala.collection.immutable.List$SerializationProxy")
            case _ => super.realClass(elementName)
          }
          debug(s"realClass($elementName)=$clazz")
          clazz
        }

//        override def realMember(`type`: Class[_], serialized: String): String = {
//          val className = `type`.getName
//          debug(s"realMember(${`type`}, $serialized)")
//          (className, serialized) match {
//            case ("scala.collection.immutable.$colon$colon", "scala.collection.immutable.$colon$colon")
//            =>  "head"
//            case _ => super.realMember(`type`, serialized)
//          }
//        }
//
        override def shouldSerializeMember(definedIn: Class[_], fieldName: String): Boolean = {
          debug(s"shouldSerializeMember($definedIn, $fieldName)")
          if(fieldName == "unserializable-parents")
            return false

          super.shouldSerializeMember(definedIn, fieldName)
        }
      }
    }
  }

}
