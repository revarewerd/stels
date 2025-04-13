package ru.sosgps.wayrecall.utils.io

import java.nio.file.{WatchEvent, StandardWatchEventKinds, FileSystems, Path}
import java.nio.file._
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.mutable
import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.utils.funcLoadingCache
import java.io.File

/**
 * Created by nickl on 09.12.13.
 */
object FileChangeListener extends grizzled.slf4j.Logging {

  type Listener = (File, WatchEvent.Kind[_]) => Unit

  private[this] val fileListeners = new mutable.HashMap[File, mutable.Set[Listener]] with mutable.MultiMap[File, Listener]


  private[this] val watcherCache = CacheBuilder.newBuilder()
    .buildWithFunction((path: Path) => {
    Files.createDirectories(path)
    val newWatchService = FileSystems.getDefault().newWatchService()
    path.register(newWatchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE)
    new Thread(new Runnable {
      def run() {
        try {
          var work = true;

          while (work) {
            val key = newWatchService.take()

            for (event <- key.pollEvents()) {

              if (event.kind() != StandardWatchEventKinds.OVERFLOW) {
                val ev = event.asInstanceOf[WatchEvent[Path]]
                val fileName = ev.context()
                val file = path.resolve(fileName).toFile
                debug("modifiedFIleContext: file=" + file)
                //fileListeners.get(file).foreach(l => l.foreach(p => p(file, event.kind())))

                for (l <- fileListeners.get(file);
                     p <- l
                ) {
                  try {
                    p(file, event.kind())
                  }
                  catch {
                    case e: Exception => warn("exception in listener " + file, e)
                  }
                }

              }

            }

            if (!key.reset()) {
              warn("stoppedWatching File")
              work = false
            }
          }
        }
        catch {
          case e: Exception => error("WatchService thread error", e)
        }
      }
    }, "WatchService thread " + path).start()

    newWatchService

  })


  def addFileistener(confDir: Path, confFileName: String, listener: Listener) {
    watcherCache(confDir)
    fileListeners.addBinding(confDir.resolve(confFileName).toFile, listener)
  }

  def addFileListener(file: File, listener: Listener) {
    watcherCache(file.getParentFile.toPath)
    fileListeners.addBinding(file, listener)
  }

}
