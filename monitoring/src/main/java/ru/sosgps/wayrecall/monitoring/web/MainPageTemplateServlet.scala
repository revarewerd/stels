package ru.sosgps.wayrecall.monitoring.web

import java.util

import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.loader.ServletLoader
import javax.servlet.ServletConfig
import javax.servlet.http.HttpServletResponse
import ru.sosgps.wayrecall.core.InstanceConfig
import ru.sosgps.wayrecall.utils.web.{BasicScalaServlet, ScalaHttpRequest}

class MainPageTemplateServlet extends BasicScalaServlet {
  private[this] var engine: PebbleEngine = null

  override def init(config: ServletConfig): Unit = {
    super.init(config)

    val engineBuilder = new PebbleEngine.Builder
    val l = new ServletLoader(config.getServletContext)
    engineBuilder.loader(l)
    this.engine = engineBuilder.build()
  }

  override protected def processRequest(request: ScalaHttpRequest, response: HttpServletResponse): Unit = {

    response.setContentType("text/html; charset=UTF-8")
    val writer = response.getWriter
    try {
      val template = engine.getTemplate("/index.html")

      val context = new util.HashMap[String, AnyRef]()
      context.put("properties", getSpringBean[InstanceConfig]().properties)

      template.evaluate(writer, context)
    } finally writer.close()

  }
}
