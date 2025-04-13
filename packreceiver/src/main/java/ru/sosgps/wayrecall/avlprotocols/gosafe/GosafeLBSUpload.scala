package ru.sosgps.wayrecall.avlprotocols.gosafe

import java.net.Socket
import ru.sosgps.wayrecall.utils.io.Utils
import scala.io.Source
import ru.sosgps.wayrecall.avlprotocols.gosafe.GosafeParser._
import ru.sosgps.wayrecall.avlprotocols.gosafe.GosafeBinaryPacket
import java.io.{File, PrintWriter}
import org.springframework.context.support.ClassPathXmlApplicationContext
import ru.sosgps.wayrecall.packreceiver.PackProcessor
import ru.sosgps.wayrecall.utils.web.ScalaJson
import java.util
import java.util.Date
import java.text.SimpleDateFormat
import ru.sosgps.wayrecall.data.sleepers.LBSHttp

/**
 * Created by nickl on 21.01.14.
 */
object GosafeLBSUpload {


  def main0(args: Array[String]) {

    val socket = new Socket("localhost", 9086)

    val stream = socket.getOutputStream

    val s3 = Utils.asBytesHex("F8" + "02010353301056526200020b19390a1000fa013202170d03040202000004040000c20005060371179f2213164c" + "F8")

    stream.write(s3)

    val istream = socket.getInputStream

    var read = istream.read()
    while (read != -1) {
      println(read);
      read = istream.read()
    }


  }

  val pattern = """.*GosafeBinaryPacket\(([0-9a-f]+)\).*""".r

  def main(args: Array[String]) {

    def arg(i: Int) = if (args.isDefinedAt(i)) Some(args(i)) else None

    val lbsconv = new LBSHttp

    val data = Source.fromFile(arg(0).get).getLines()
    //val context = new ClassPathXmlApplicationContext("receiver-spring.xml")
    try {
      //val packProcessor = context.getBean(classOf[PackProcessor])

      val binPacketData = data.collect({case pattern(e) => e})

      val out = new PrintWriter("gpss2.txt")

      for ((bd, i) <- binPacketData.zipWithIndex) {
        //println(bd)
        val s3 = Utils.asBytesHex(bd) //
        val p = parseBinaryPacket(new GosafeBinaryPacket(s3))

        println(i + " " + p.addata)
        val gpsDatas = p.gpsData ++ convertLBSToGpsData(lbsconv, p)
        for (gpsData <- gpsDatas /* if gpsData.imei == "359394050338486"*/ ) {
          appendAdditionalData(p, gpsData)
          println(gpsData)
          println(gpsData.data)
        }

        //        out.println(gpsData)
        //        try {
        //          packProcessor.addGpsData(gpsData)
        //        }
        //        catch {
        //          case e: Throwable => e.printStackTrace()
        //        }
      }

      out.close()
    }
    //finally
    //context.close()

    //    for (row <- data) {
    //      println(row)
    //      row match {
    //        case pattern(e) => println(e)
    //        case _ =>
    //      }
    //    }

  }

  //  def main(args: Array[String]) {
  //
  //    import collection.JavaConversions.mapAsScalaMap
  //    import collection.JavaConversions.asScalaBuffer
  //    import ru.sosgps.wayrecall.utils.typingMap
  //    import ru.sosgps.wayrecall.utils.typingMapJava
  //
  //    type jm = util.Map[String, AnyRef]
  //    type jl = util.List[util.Map[String, AnyRef]]
  //
  //    val resp = ScalaJson.parse[Seq[jm]](new File("/home/nickl/NetBeansProjects/forStels/repo/gpss.json"))
  //
  //    val records = resp.head.as[jm]("result").as[jl]("records")
  //
  //    val format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
  //
  //    for (rec <- records) {
  //
  //      val date = new Date(rec.as[java.lang.Long]("timemils"))
  //
  //
  //
  //      println(Seq(format.format(date),rec.get("coordinates"),rec.get("regeo"),rec.get("devdata")).mkString("\t"))
  //    }
  //
  //  }


  def collectGoSafeBP(data: Iterator[String]): Iterator[String] = {
    data.collect({case pattern(e) => e})
  }
}
