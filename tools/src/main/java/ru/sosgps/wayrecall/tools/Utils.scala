package ru.sosgps.wayrecall.tools

import java.io._
import java.net.InetAddress
import java.util.Properties

import com.mongodb.casbah.Imports._
import org.bson.{BasicBSONDecoder, BasicBSONEncoder}
import com.mongodb.util.JSONCallback

/**
  * Created with IntelliJ IDEA.
  * User: nickl
  * Date: 22.05.13
  * Time: 20:27
  * To change this template use File | Settings | File Templates.
  */
object Utils {

  def iterateStream(stream: InputStream): Stream[DBObject] = {
    val decoder = new BasicBSONDecoder

    def streamMaker: Stream[DBObject] = {
      try {
        val cb = new JSONCallback()
        decoder.decode(stream, cb)
        var dbo = cb.get().asInstanceOf[DBObject]
        dbo #:: streamMaker
      }
      catch {
        case e: Exception => {
          stream.close()
          Stream.empty
        }
      }
    }

    streamMaker
  }

  def readSerializedData[T](filename: String): Stream[T] = {
    ru.sosgps.wayrecall.utils.io.iterateStream(new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename))), {
      s: ObjectInputStream => s.readObject().asInstanceOf[T]
    })
  }

  def storeSerializableData(result: TraversableOnce[_], filename: String) {
    val out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filename)))
    try {
      for (entry <- result) {
        out.writeObject(entry)
      }
    }
    finally {
      out.close()
    }
  }

}

object ConnectionUtils {

  def readAddressOrUseLocal(sendAddress: String, configPropName: String): (InetAddress, Int) = {
    val serverAddress: String = Option(sendAddress).getOrElse({
      val properties = new Properties()
      properties.load(new FileInputStream(System.getenv("WAYRECALL_HOME") + "/conf/packreceiver.properties"))
      "localhost:" + properties.getProperty(configPropName)
    })
    val addresport = serverAddress.split(":")

    //check existence
    addresport(0)
    addresport(1)

    val address: InetAddress = InetAddress.getByName(addresport(0))
    val port: Int = addresport(1).toInt
    (address, port)
  }


}


