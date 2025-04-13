package ru.sosgps.wayrecall.initialization

import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}

/**
 * Created by nickl on 28.09.14.
 */
class VhostLister(mscfg: MultiserverConfig) extends HttpServlet{
  override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = process(req, resp)

  override def doPost(req: HttpServletRequest, resp: HttpServletResponse): Unit = process(req, resp)

  def process(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
     resp.setContentType("text/plain")
    val suffixes = mscfg.gloabalProps.getProperty("global.vhostsuffixes","").split(",").toIndexedSeq.map(_.trim)
    val writer = resp.getWriter
    try{
      mscfg.instances.flatMap(_.vhosts).filter(vhost => suffixes.exists(vhost.endsWith) ).foreach(writer.println)
    }
    finally {
      writer.close()
    }

  }
}
