package ru.sosgps.wayrecall.monitoring.web

import java.awt.{BasicStroke, Color, Image, RenderingHints}
import java.awt.image.BufferedImage
import java.io.{File, OutputStream}
import java.nio.file.Files
import java.util.{Date, UUID}
import javax.imageio.ImageIO
import javax.servlet.ServletContext
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.{Component, Controller}
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestParam}
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.monitoring.OSMTilesCache

import scala.concurrent.duration.DurationInt
import scala.concurrent.Await

/**
  * Created by ivan on 11.03.16.
  */
@Controller
class ReportMapGenerator extends grizzled.slf4j.Logging {
  val staticImagesStore = new StaticImagesStore

  @Autowired
  var osmTiles: OSMTilesCache = null

  val tileSize = 256

  def getXY(lat: Double, lon: Double, zoom: Int) = {
    var xtile = ((lon + 180) / 360 * (1 << zoom))
    var ytile = ((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom))
    if (xtile < 0) xtile = 0
    if (xtile >= (1 << zoom)) xtile = ((1 << zoom) - 1)
    if (ytile < 0) ytile = 0
    if (ytile >= (1 << zoom)) ytile = ((1 << zoom) - 1)
    (xtile, ytile)
  }

  def getTileNumber(lat: Double, lon: Double, zoom: Int) = {
    var xtile: Int = Math.floor((lon + 180) / 360 * (1 << zoom)).asInstanceOf[Int]
    var ytile: Int = Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom)).asInstanceOf[Int]
    if (xtile < 0) xtile = 0
    if (xtile >= (1 << zoom)) xtile = ((1 << zoom) - 1)
    if (ytile < 0) ytile = 0
    if (ytile >= (1 << zoom)) ytile = ((1 << zoom) - 1)
    (xtile, ytile)
  }

  def minMaxLatLon(history: Iterable[GPSData]): (Double, Double, Double, Double) = {
    var minLat = history.head.lat
    var minLon = history.head.lon
    var maxLat = history.head.lat
    var maxLon = history.head.lon

    for (gps <- history) {
      if (gps.lat < minLat)
        minLat = gps.lat
      if (gps.lat > maxLat)
        maxLat = gps.lat
      if (gps.lon < minLon)
        minLon = gps.lon
      if (gps.lon > maxLon)
        maxLon = gps.lon
    }
    (minLat, minLon, maxLat, maxLon)
  }

  def genZoomBoundingBox(t: (Double, Double, Double, Double), tiles: Int): Int = {
    t match {
      case (minLat: Double, minLon: Double, maxLat: Double, maxLon: Double) =>
        for (zoom <- 17.to(1, -1)) {
          var (minx, miny) = getTileNumber(minLat, minLon, zoom)
          var (maxx, maxy) = getTileNumber(maxLat, maxLon, zoom)
          debug("maxx minx zoom=" + maxx + ", " + minx + ", " + zoom)
          if (Math.abs(maxx - minx) < tiles)
            return zoom
        }
        10
    }
  }

  def getRatio(maxx: Int, minx: Int, maxy: Int, miny: Int) = (maxx - minx + 1).toDouble / (maxy - miny + 1)


  def tilesDiapazoneAndZoom(mmLonLat: (Double, Double, Double, Double), tiles: Int, ratioOpt: Option[Double] = None): ((Int, Int, Int, Int), Int) = {
    var selectedZoom: Int = 10
    mmLonLat match {
      case (minLat: Double, minLon: Double, maxLat: Double, maxLon: Double) =>
        for (zoom <- 17.to(1, -1)) {
          var (minx, miny) = getTileNumber(minLat, minLon, zoom)
          var (maxx, maxy) = getTileNumber(maxLat, maxLon, zoom)
          if (minx > maxx) {
            val t = maxx
            maxx = minx
            minx = t
          }

          if (miny > maxy) {
            val t = maxy
            maxy = miny
            miny = t
          }

          debug(s"minx: $minx maxx: $maxx miny $miny maxy $maxy")
          debug("ratioOpt: " + ratioOpt)

          def closestEven(x: Int) = if (x % 2 == 0) x else x + 1
          if(ratioOpt.isDefined) {
            val ratio = ratioOpt.get
            var currentRatio = getRatio(maxx,minx,maxy,miny)
            debug(s"current ratio: $currentRatio")
            val hTiles = maxx - minx + 1
            val vTiles = maxy - miny + 1
            debug(s"htiles:$hTiles vtiles:$vTiles")
            if(currentRatio < ratio) {
              val newHTiles = math.ceil(vTiles * ratio).toInt
              debug(s"newHtiles: $newHTiles")
              val tilesDiff = closestEven(newHTiles - hTiles)
              minx -= (tilesDiff / 2)
              maxx += tilesDiff / 2
            }
            else {
              val newVTiles = math.ceil(hTiles / ratio).toInt
              debug(s"newVTiles: $newVTiles")
              val tilesDiff = closestEven(newVTiles - vTiles)
              miny -= (tilesDiff / 2)
              maxy += tilesDiff / 2
            }

          }




          debug("maxx minx zoom=" + maxx + ", " + minx + ", " + zoom)
          if (Math.abs(maxx - minx) < tiles)
            return ((minx, miny, maxx, maxy), zoom)
        }
        throw new RuntimeException("Failed to find an appropriate bounding box")
    }
  }

  def tilesDiapazone(t: (Double, Double, Double, Double), zoom: Int): ((Int, Int), (Int, Int)) =
    tilesDiapazone(t._1, t._2, t._3, t._4, zoom)


  // Интегрировать с ratio
  def tilesDiapazone(minLat: Double, minLon: Double, maxLat: Double, maxLon: Double, zoom: Int): ((Int, Int), (Int, Int)) = {
    var (minx, miny) = getTileNumber(minLat, minLon, zoom)
    var (maxx, maxy) = getTileNumber(maxLat, maxLon, zoom)

    if (minx > maxx) {
      val t = maxx
      maxx = minx
      minx = t
    }

    if (miny > maxy) {
      val t = maxy
      maxy = miny
      miny = t
    }
    ((minx, miny), (maxx, maxy))
  }

  def getStrokeSize(zoom: Int) = {
    if(zoom > 15)
      6.0f
    else if (zoom > 13)
      5.0f
    else
      4.0f
  }
  def genPathImage(history: Iterable[GPSData], ratioOpt: Option[Double] = None): BufferedImage = {
    var mmlonlat = minMaxLatLon(history)

    debug("mmlonlat:" + mmlonlat)

    val ((minx, miny, maxx, maxy), zoom) = tilesDiapazoneAndZoom(mmlonlat, 8, ratioOpt)
 //   val zoom = genZoomBoundingBox(mmlonlat, 8)
    debug("zoom:" + zoom)
    //var ((minx, miny), (maxx, maxy)) = tilesDiapazone(mmlonlat, zoom)


    debug("mmxy:" +(minx, miny, maxx, maxy))
    val width = (maxx - minx + 1) * tileSize
    val height = (maxy - miny + 1) * tileSize
    debug("width:" + width + " height:" + height + " w * h =" + width * height)

    if (width * height < 0 || width * height > 20000000) {
      val rimage = new BufferedImage(
        256,
        256,
        BufferedImage.TYPE_INT_RGB);
      val graphics = rimage.createGraphics()
      graphics.drawString("Изображение слишком велико", 40, 50)
      graphics.dispose()
      return rimage
    }

    val rimage = new BufferedImage(
      width,
      height,
      BufferedImage.TYPE_INT_RGB)

    val graphics = rimage.createGraphics()
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    for (y <- miny to maxy) {
      for (x <- minx to maxx) {
        val img = loadImage(zoom, x, y)
        graphics.drawImage(img, (x - minx) * tileSize, (y - miny) * tileSize, null)

        // Сетка из тайлов
        //graphics.drawRect((x - minx) * tileSize, (y - miny) * tileSize, tileSize, tileSize )
      }
    }


    val s = new BasicStroke(getStrokeSize(zoom));
    graphics.setStroke(s)
    val color = new Color(0xAF0000E1, true)
    graphics.setColor(color)
    val coords = history.map(gps => getXY(gps.lat, gps.lon, zoom))
    if(coords.nonEmpty) {
      val first = coords.head
      var last = first
      for (g <- coords.sliding(2)) {
        val i = g.iterator
        val (x1, y1) = i.next()
        val (x2, y2) = i.next()
        last = (x2, y2)
        graphics.drawLine(((x1 - minx) * tileSize).toInt, ((y1 - miny) * tileSize).toInt, ((x2 - minx) * tileSize).toInt, ((y2 - miny) * tileSize).toInt)
      }
      graphics.setColor(Color.RED)
      first match  {
        case (x,y) => {
          val image = ImageIO.read(new File(servletContext.getRealPath("images/ico24_flag_str.png")))
          graphics.drawImage(image, ((x - minx) * tileSize).toInt,((y - miny) * tileSize).toInt, null)
        }
      }
      graphics.setColor(Color.CYAN)
      last match {
        case (x, y) => {
          val image = ImageIO.read(new File(servletContext.getRealPath("images/ico24_flag_fin.png")))
          graphics.drawImage(image, ((x - minx) * tileSize).toInt, ((y - miny) * tileSize).toInt, null)
        }
      }
    }


    graphics.dispose()
    rimage
  }

  @Autowired
  var servletContext:ServletContext = _



  def loadImage(zoom: Int, x: Int, y: Int): BufferedImage = {
    //    val imgurl = "http://b.tile.openstreetmap.org/" + zoom + "/" + x + "/" + y + ".png";
    //    val img = ImageIO.read(new URL(imgurl))
    //    img
    Await.result(osmTiles.getImage(zoom, x, y), 120.seconds)
  }

  private def getUUID(uid: String, from: Date, to: Date): String = {
    uid + "-" + from.getTime + "-" + to.getTime
  }

  def loadOrGenerate(uuid: String, history: Iterable[GPSData]): File = {
    val file = staticImagesStore.getFile(uuid)
    if (!file.exists() || !(file.lastModified() > System.currentTimeMillis() - 5 * 60 * 1000)) {
      val rimage = genPathImage(history)
      staticImagesStore.save(rimage, uuid)
    }
    file
  }


  @RequestMapping(Array("/staticimg/{uuid}.png"))
  def process(@PathVariable("uuid") uuid: String,
              response: HttpServletResponse) {
    response.setContentType("image/png")

    val writer = response.getOutputStream
    try {
      staticImagesStore.loadToStream(uuid, writer)
    }
    finally {
      writer.close()
    }

  }

  @RequestMapping(Array("/osm/{zoom}/{x}/{y}.png"))
  def osm(
           @PathVariable("zoom") zoom: Int,
           @PathVariable("x") x: Int,
           @PathVariable("y") y: Int,
           response: HttpServletResponse) {
    try {
      response.setContentType("image/png")
      response.setDateHeader("Expires", System.currentTimeMillis + 1000 * 60 * 60 * 24)
      response.setHeader("Cache-Control:", "max-age=27632")

      val writer = response.getOutputStream
      try {
        osmTiles.writeToStreamImage(zoom, x, y, writer)
      }
      finally {
        writer.close()
      }
    } catch {
      case e: org.eclipse.jetty.io.EofException => throw e
      case e: Exception => {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
        response.setContentType("plain/text")
        warn("osm error", e)
        val writer = response.getWriter
        e.printStackTrace(writer)
        writer.close()
      }
    }

  }

  @Autowired
  var packStore: PackagesStore = _

  @RequestMapping(Array("/reportMap"))
  def generateImage(request: HttpServletRequest, response: HttpServletResponse, @RequestParam("uid") uid: String, @RequestParam("from") longFrom: Long, @RequestParam("to") longTo: Long): Unit = {
    val (from,to) = (new Date(longFrom), new Date(longTo))
    val widthOpt = Option(request.getParameter("width")).map(_.toInt)
    val heightOpt = Option(request.getParameter("height")).map(_.toInt)

    response.setContentType(MediaType.IMAGE_PNG_VALUE)

    val history = packStore.getHistoryFor(uid, from, to)

    val image = if(widthOpt.isDefined)
      genPathImage(history, widthOpt.map(w => w.toDouble / heightOpt.get))
    else
      genPathImage(history)

    ImageIO.write(image, "png", response.getOutputStream)
  }

}

class StaticImagesStore {

  val directory = {
    val file = new File("staticImgTempStore")
    file.mkdirs()
    file
  }

  def save(img: BufferedImage, uuid: String = UUID.randomUUID().toString): String = {
    ImageIO.write(img, "png", getFile(uuid))
    uuid
  }

  def getFile(uuid: String): File = {
    new File(directory, uuid + ".png")
  }

  def loadToStream(uuid: String, out: OutputStream) = {
    Files.copy(new File(directory, uuid + ".png").toPath, out)
  }


}
