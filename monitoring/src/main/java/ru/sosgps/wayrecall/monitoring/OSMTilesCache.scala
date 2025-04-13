package ru.sosgps.wayrecall.monitoring

import java.awt.image.BufferedImage
import java.io.{OutputStream, File}
import java.net.URL
import java.nio.file.{Path, Paths, Files}
import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}
import javax.annotation.PreDestroy
import javax.imageio.ImageIO

import com.google.common.cache.{LoadingCache, CacheBuilder, Cache}
import com.google.common.util.concurrent.MoreExecutors
import org.apache.http.HttpResponse
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.nio.client.{HttpAsyncClientBuilder, HttpAsyncClients}
import org.eclipse.jetty.servlet.DefaultServlet
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService
import ru.sosgps.wayrecall.utils.funcLoadingCache

import scala.concurrent._
import scala.concurrent.duration.DurationInt

/**
 * Created by nickl on 28.06.14.
 */
class OSMTilesCache extends grizzled.slf4j.Logging {

  private val _client = HttpAsyncClientBuilder.create()
    .setMaxConnTotal(10)
    .setMaxConnPerRoute(10)
    .setDefaultRequestConfig(RequestConfig.custom()
    .setConnectionRequestTimeout(15000)
    .setConnectTimeout(15000)
    .build()
    ).build()


  import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

  //  private val execcontext = new ScalaExecutorService("OSMTilesCache",1,10,false,10,TimeUnit.MINUTES,
  //    new ArrayBlockingQueue[Runnable](5))

  private def client = {
    if (!_client.isRunning)
      _client.start()
    _client
  }

  val directory = {
    Files.createDirectories(Paths.get("osmTilesCache"))
  }

  private[this] val imageTasks: LoadingCache[(Int, Int, Int), Future[BufferedImage]] = CacheBuilder.newBuilder()
    .expireAfterAccess(30, TimeUnit.SECONDS)
    .buildWithFunction[(Int, Int, Int), Future[BufferedImage]]({ case (zoom: Int, x: Int, y: Int) => {
    val result = promise[BufferedImage]()
    val path = getPathFor(zoom, x, y)
    if (Files.exists(path)) {
      result.success(ImageIO.read(path.toFile))
    }
    else {
      val downloadFuture = downloadImage(zoom: Int, x: Int, y: Int)
      downloadFuture.onSuccess({ case img =>
        Files.createDirectories(path.getParent)
        val pathtemp = path.getParent.resolve(path.getFileName.toString + "-temp")
        //debug("writing: " + pathtemp)
        ImageIO.write(img, "png", pathtemp.toFile)
        //debug("renaming: " + path)
        Files.move(pathtemp, path)
        //debug("renamed")
      })
      result.completeWith(downloadFuture)
    }
    val r = result.future
    r.onFailure({ case e => imageTasks.invalidate((zoom, x, y))})
    r
  }
  })

  def getImage(zoom: Int, x: Int, y: Int): Future[BufferedImage] = imageTasks.get((zoom, x, y))

  def getPathFor(zoom: Int, x: Int, y: Int): Path = {
    val path = directory.resolve(zoom.toString).resolve(x.toString).resolve(y.toString + ".png")
    path
  }

  def writeToStreamImage(zoom: Int, x: Int, y: Int, out: OutputStream) {
    val path = getPathFor(zoom, x, y)
    if (Files.exists(path)) {
      Files.copy(path, out)
    }
    else {
      val image = Await.result(getImage(zoom, x, y), 30.second)
      ImageIO.write(image, "png", out)
    }

  }

  def downloadImage(zoom: Int, x: Int, y: Int): Future[BufferedImage] = {

    val result = promise[BufferedImage]()

    client.execute(new HttpGet("http://b.tile.openstreetmap.org/" + zoom + "/" + x + "/" + y + ".png"), new FutureCallback[HttpResponse] {

      def cancelled(): Unit = result.failure(new IllegalStateException("cancelled"))

      def completed(p1: HttpResponse): Unit = {
        result.success(ImageIO.read(p1.getEntity.getContent))
      }

      def failed(p1: Exception): Unit = {
        result.failure(p1)
      }
    })

    result.future
  }

  @PreDestroy
  def close() {
    _client.close()
  }


}
