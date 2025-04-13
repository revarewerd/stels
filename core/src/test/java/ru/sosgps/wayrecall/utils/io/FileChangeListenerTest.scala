package ru.sosgps.wayrecall.utils.io

import java.io.File

/**
 * Created by nickl on 09.12.13.
 */
object FileChangeListenerTest {


  def main(args: Array[String]) {

    FileChangeListener.addFileistener(new File("/home/nickl").toPath, "ttt.txt", (f, e) => {
      println("fsevent:" + f + " " + e)
    })

    FileChangeListener.addFileListener(new File("/home/nickl/ttt2.txt"), (f, e) => {
      println("fsevent2:" + f + " " + e)
    })

    FileChangeListener.addFileListener(new File("/home/nickl/dump/ttt2.txt"), (f, e) => {
      println("fsevent3:" + f + " " + e)
    })

  }

}
