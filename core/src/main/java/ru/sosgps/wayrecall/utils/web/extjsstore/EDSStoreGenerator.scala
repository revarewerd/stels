package ru.sosgps.wayrecall.utils.web.extjsstore

import org.springframework.stereotype.Controller
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import org.springframework.context.ApplicationContext
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.utils.ExtDirectService
import collection.JavaConversions.mapAsScalaMap
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}
import java.lang.reflect.Method
import java.util.NoSuchElementException
import scala.NoSuchElementException


@Controller
class EDSStoreGenerator extends grizzled.slf4j.Logging {

  @Autowired
  var appContext: ApplicationContext = _

  private[extjsstore] lazy val storesProcessors: Map[String, DescriptorInfo] = {
    require(appContext != null, "appContext is null")
    (
      for ((n, s) <- appContext.getBeansWithAnnotation(classOf[ExtDirectService]);
           if (s.isInstanceOf[EDSStoreServiceDescriptor])
      )
      yield {
        val descr = s.asInstanceOf[EDSStoreServiceDescriptor]
        (descr.name, DescriptorInfo(descr.name, n, descr))
      }).toMap
  }

  protected val prefix = "EDS"

  @RequestMapping(value = Array("/{midpart}/{name}.js"))
  def generateJs(@PathVariable("midpart") midpart: String,
                 @PathVariable("name") name: String,
                 request: HttpServletRequest, response: HttpServletResponse) = try {
    //TODO: сделать редирект другому котроллеру вместо выбрасывания исключения
    val descr = storesProcessors.getOrElse(name, throw new NoSuchElementException(name + " was not found"))
    response.setContentType("application/x-javascript; charset=utf-8")
    val out = response.getWriter()
    try {

      out.println( """
Ext.define('""" + prefix + """.model.""" + name + """', {
    extend:'Ext.data.Model',
    fields:""" + descr.descriptor.model.fields.map(f => f.typ match {
        case "auto" => "'" + f.name + "'";
        case _ => "{name: '" + f.name + "', type: '" + f.typ + "'}"
      }).mkString("[", ",", "]") + """,
    idProperty:'""" + descr.descriptor.idProperty + """'
});

Ext.define('""" + prefix + """.store.""" + name + """', {
    extend:'""" + descr.descriptor.storeType + """',
    model:'""" + prefix + """.model.""" + name + """',
    constructor : function(config) {
               config = Ext.apply({
                       proxy:{
                          type:'direct',""" +
        (if (!descr.descriptor.lazyLoad) "limitParam:undefined," else "")
        //                          limitParam:undefined,
        + """
          """ + genAPI(descr.beanName, descr.descriptor) + """,
                          reader:{
                              root:'records'
                          },
                          listeners:{
                                     exception:function (proxy, response, operation) {
                                         Ext.MessageBox.show({
                                             title:'Произошла ошибка',
                                             msg:operation.getError(),
                                             icon:Ext.MessageBox.ERROR,
                                             buttons:Ext.Msg.OK
                                         })
                                     }
                                 }
                      }
            }, config);
            this.superclass.constructor.call(this, config);
        },
    nodeParam: '""" + descr.descriptor.idProperty + """',
    autoSync:""" + descr.descriptor.autoSync + """,
    buffered:""" + descr.descriptor.lazyLoad + """,""" +
        (if (descr.descriptor.lazyLoad) "\n    pageSize:200," else "") + """
    remoteSort:""" + descr.descriptor.lazyLoad + """,
    remoteFilter:""" + descr.descriptor.lazyLoad + """
});
                                                   """)
    }
    finally {
      out.close()
    }


  } catch {
    case e: NoSuchElementException => {
      warn("not found page:" + e + " " + e.getMessage)
      response.setStatus(HttpServletResponse.SC_NOT_FOUND)
    }
  }

  protected def genAPI(beanName: String, descr: EDSStoreServiceDescriptor): String = {

    val methodsWithAnnotations = descr.getClass.getMethods.toSeq.flatMap(method => Option(method.getAnnotation(classOf[ExtDirectMethod])).map(ann => (method, ann)))

    val methodsByAnnotations = methodsWithAnnotations.groupBy(_._2.value())

    def requireOnlyOne[T](i: Iterable[T], name: => String): T = {
      require(i.tail.isEmpty, name);
      i.head
    }

    val readMethod = methodsByAnnotations.get(ExtDirectMethodType.STORE_READ)
      .orElse(methodsByAnnotations.get(ExtDirectMethodType.TREE_LOAD))
      .map(requireOnlyOne(_, "readMethod")).map(_._1.getName)

    val modifyMethods: Map[String, (Method, ExtDirectMethod)] =
      methodsByAnnotations.get(ExtDirectMethodType.STORE_MODIFY)
        .map(tuples =>
        tuples.groupBy(
          tuple => Option(tuple._1.getAnnotation(classOf[StoreAPI])).map(_.value()).orNull
        ).-(null).map(kv => (kv._1, requireOnlyOne(kv._2, kv._1)))
        ).getOrElse(Map.empty)

    val sb = new StringBuilder()
    sb.append("api:{")
    sb.append("\n")
    val methods = readMethod.map(method => {
      ("   " + "read" + ":" + beanName + "." + method)
    }) ++
      (for ((k, v) <- modifyMethods) yield {
        ("   " + k + ":" + beanName + "." + v._1.getName)
      })
    sb.append(methods.mkString("", ",\n", "\n"))

    sb.append("}");
    sb.toString()
  }


}


private[extjsstore] case class DescriptorInfo(name: String, beanName: String, descriptor: EDSStoreServiceDescriptor)


trait EDSStoreServiceDescriptor {

  implicit def strToField(str: String) = Field(str)

  val model: Model

  val name: String

  val idProperty: String

  val lazyLoad: Boolean

  val storeType: String = "Ext.data.Store"

  val autoSync: Boolean = true

  class Model(val fields: Seq[Field]) {
    val fieldsNames: Set[String] = fields.map(_.name).toSet
  }

  object Model {
    def apply(fs: Field*) = new Model(fs.toSeq)
  }

  case class Field(name: String, typ: String = "auto")

}
   
