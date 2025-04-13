package ru.sosgps.wayrecall.utils.web.extjsstore

import java.lang.reflect.Method
import java.util.NoSuchElementException
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import ru.sosgps.wayrecall.utils.ExtDirectService

/**
 * Created by IVAN on 22.12.2014.
 */

@Controller
@RequestMapping(value = Array("/Ext5"))
class ExtJs5EDSStoreGenerator  extends EDSStoreGenerator {

  override val prefix = "EDS.Ext5"

  @RequestMapping(value = Array("/{midpart}/{name}.js"))
  override def generateJs(@PathVariable("midpart") midpart: String,
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
                              rootProperty:'records'
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
}