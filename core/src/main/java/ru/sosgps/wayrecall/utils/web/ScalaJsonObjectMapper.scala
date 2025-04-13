package ru.sosgps.wayrecall.utils.web


import com.fasterxml.jackson.core.`type`.TypeReference
import org.bson.types.ObjectId
import org.springframework.core.convert.converter.Converter
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser, Version}
import com.fasterxml.jackson.databind.{BeanDescription, BeanProperty, DeserializationFeature, JavaType, JsonSerializer, Module, ObjectMapper, SerializationConfig, SerializerProvider}
import com.fasterxml.jackson.databind.ser.Serializers
import java.util

import com.fasterxml.jackson.module.scala._
import java.io._
import java.net.URL

import com.fasterxml.jackson.module.scala.deser.UntypedObjectDeserializerModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule
import com.google.common.base.Charsets
import org.apache.commons.io.IOUtils
import ru.sosgps.wayrecall.utils.errors.BadRequestException

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 12.08.12
 * Time: 14:00
 * To change this template use File | Settings | File Templates.
 */


/*
You can also mixin ScalaObjectMapper (experimental) to get rich wrappers that automatically convert scala manifests directly into TypeReferences for Jackson to use:

val mapper = new ObjectMapper() with ScalaObjectMapper
mapper.registerModule(DefaultScalaModule)
val myMap = mapper.readValue[Map[String,Tuple2[Int,Int]]](src)
 */

class ScalaJsonObjectMapper extends ObjectMapper with ScalaJsonApi{
  registerModule(new JacksonModule
    with IteratorModule
    with EnumerationModule
    with OptionModule
    with SeqModule
    with IterableModule
    with TupleModule
    with MapModule
    with SetModule
    with ScalaAnnotationIntrospectorModule
    )
  registerModule(new WayRecallJacksonMapperModule())
  configure(JsonParser.Feature.ALLOW_COMMENTS, true)
  configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  override protected def objectMapper: ObjectMapper = this
}

@deprecated("use ScalaCollectionJson")
object ScalaJson extends ScalaJsonObjectMapper

/**
 * Convets all to Scala Collections
 */
object ScalaCollectionJson extends ObjectMapper with ScalaJsonApi{
  registerModule(DefaultScalaModule)
  configure(JsonParser.Feature.ALLOW_COMMENTS, true)

  override protected def objectMapper: ObjectMapper = this
}


private class WayRecallJacksonMapperModule() extends Module {

  //import org.codehaus.jackson.map._
  //  import com.fasterxml.jackson.`type`.JavaType
  //  import com.fasterxml.jackson.Version
  //  import com.fasterxml.jackson.map.Module.SetupContext
  //  import org.bson.types.ObjectId
  //  import com.fasterxml.jackson.JsonGenerator

  def version = new Version(0, 0, 1, "")

  def getModuleName = "WayRecallJacksonMapperModule"

  def setupModule(context: Module.SetupContext) {
    context.addSerializers(new Serializers)
  }

  //com.fasterxml.jackson.databind.ser.Serializers.Base
  class Serializers extends com.fasterxml.jackson.databind.ser.Serializers.Base {

    // public com.fasterxml.jackson.databind.JsonSerializer<Object> findSerializer
    // (com.fasterxml.jackson.databind.SerializationConfig config,
    //  com.fasterxml.jackson.databind.JavaType javaType,
    //   com.fasterxml.jackson.databind.BeanDescription beanDesc) {

    override def findSerializer(config: com.fasterxml.jackson.databind.SerializationConfig,
                                javaType: com.fasterxml.jackson.databind.JavaType,
                                beanDesc: com.fasterxml.jackson.databind.BeanDescription
                                 ): JsonSerializer[Object] = {
      val ser: Object = if (classOf[ObjectId].isAssignableFrom(beanDesc.getBeanClass)) {
        new JsonSerializer[ObjectId] {
          def serialize(value: ObjectId, jgen: JsonGenerator, provider: SerializerProvider) {
            provider.defaultSerializeValue(value.toString, jgen)
          }
        }
      } else {
        null
      }
      ser.asInstanceOf[JsonSerializer[Object]]
    }
  }


}


class ScalaMapConverter extends Converter[java.util.Map[_, _], scala.collection.immutable.Map[_, _]] {
  def convert(source: util.Map[_, _]) = collection.JavaConversions.mapAsScalaMap(source).toMap
}

class ListConverter extends Converter[java.util.ArrayList[_], scala.collection.Seq[_]] {

  val smc = new ScalaMapConverter

  def convert(source: util.ArrayList[_]) = {
    val r = collection.JavaConversions.asScalaBuffer(source).toSeq
    if (r.headOption.map(e => (e.isInstanceOf[java.util.Map[_, _]]: Boolean)).getOrElse(false))
      r.map(a => smc.convert(a.asInstanceOf[util.Map[_, _]]))
    else
      r
  }
}

class MapToListConverter extends Converter[java.util.Map[_, _], scala.collection.Seq[_]] {

  val smc = new ScalaMapConverter

  def convert(p1: util.Map[_, _]) = Seq(smc.convert(p1))

}

class DateConverter extends Converter[String, java.util.Date] {

  import java.text.SimpleDateFormat
  import java.util.{Locale, Date}
  import java.util.TimeZone

  def convert(source: String): java.util.Date = {

    //    //println("TimeZone="+TimeZone.getDefault)
    //
    //    val simpledateformat = new SimpleDateFormat("EEE MMM d yyyy HH:mm:ss 'GMT'Z", Locale.US)
    //
    //    val date = simpledateformat.parse(source)
    //    //println("DateConverter:" + source + " ->" + simpledateformat.format(date) +" ("+date+")")
    ru.sosgps.wayrecall.utils.parseDate(source)
  }
}

trait ScalaJsonApi{

  protected def objectMapper: ObjectMapper

  def parse[T: Manifest](src: String): T = objectMapper.readValue[T](src, manifest[T].runtimeClass.asInstanceOf[Class[T]])

  def parse[T: Manifest](src: File): T = objectMapper.readValue[T](src, manifest[T].runtimeClass.asInstanceOf[Class[T]])

  def parse[T: Manifest](src: InputStream): T = objectMapper.readValue[T](src, manifest[T].runtimeClass.asInstanceOf[Class[T]])

  def parse[T: Manifest](src: URL): T = objectMapper.readValue[T](src, manifest[T].runtimeClass.asInstanceOf[Class[T]])

  def generate(str: AnyRef, out: OutputStream) = objectMapper.writeValue(out, str)

  def generate(str: AnyRef, out: Writer) = objectMapper.writeValue(out, str)

  def generate(str: AnyRef, out: File) = objectMapper.writeValue(out, str)

  def generate(str: AnyRef) = objectMapper.writeValueAsString(str)
}

class LoudJsonHander extends ch.ralscha.extdirectspring.util.JsonHandler{
  override def readValue[T](json: String, typeReference: TypeReference[T]): T = getMapper.readValue(json, typeReference).asInstanceOf[T];

  override def readValue(is: InputStream, clazz: Class[AnyRef]): AnyRef = {
    val string = IOUtils.toString(is, Charsets.UTF_8)
    try {
      getMapper.readValue(string, clazz)
    } catch {
      case e: Exception => throw new BadRequestException(s"cant parse as JSON: '$string'", e)
    }
  }

  override def readValue[T](json: String, clazz: Class[T]): T = getMapper.readValue(json, clazz);
}