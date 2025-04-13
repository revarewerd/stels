package ru.sosgps.wayrecall.m2msms

import ru.sosgps.wayrecall.core.MongoDBManager
import com.mongodb.casbah.Imports._
import java.util.Date
import SendSms.richMessage
import collection.JavaConversions.iterableAsScalaIterable
import ru.sosgps.wayrecall.m2msms.generated.{ArrayOfMessageInfo, MessageInfo, DeliveryStatus, RequestMessageType}
import scala.collection.mutable.ArrayBuffer
import scala.io.{BufferedSource, Source}
import org.joda.time.DateTime
import scala.collection.{TraversableLike, mutable}
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.{Font, HorizontalAlignment}
import java.io.{File, FileInputStream, FileOutputStream}
import java.util.concurrent.atomic.AtomicInteger
import SoapUtils.toDate
import ru.sosgps.wayrecall.utils.POIUtils.toTypedValue
import org.apache.poi.openxml4j.opc.{PackageAccess, OPCPackage}
import resource._

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 22.04.13
 * Time: 21:31
 * To change this template use File | Settings | File Templates.
 */
object AutoPhones {

  var real: Boolean = false

  val spring = SendSms.setupSpring();

  lazy val sendSms = spring.getBean(classOf[SendSms])

  val collection = spring.getBean(classOf[MongoDBManager]).getDatabase()("sleeping")

  lazy val colibrises = Source.fromInputStream(this.getClass.getClassLoader
    .getResourceAsStream("autophones/colibrises.txt"))

  lazy val autophone31 = Source.fromInputStream(this.getClass.getClassLoader
    .getResourceAsStream("autophones/autophone31.txt"))

  lazy val autophones44 = Source.fromInputStream(this.getClass.getClassLoader
    .getResourceAsStream("autophones/autophones44.txt"))

  lazy val autopohones56 = Source.fromInputStream(this.getClass.getClassLoader
    .getResourceAsStream("autophones/autophones56.txt"))

  //  def tendays = Source.fromInputStream(this.getClass.getClassLoader
  //    .getResourceAsStream("autophones/10days.txt"))
  def tendays = Source.fromFile(System.getenv("WAYRECALL_HOME") + "/10days.txt")

  val autophonePhoneSms = """(\w*),(\+?\d*)""".r

  val autophonePhoneSms2 = """(\w*),UNO;(\+?\d*)""".r

  val autophonePhoneSms3 = """(\w*),p=(\+?\d*)""".r

  val autophonePhoneSms4 = """(\w*),024H,F""".r

  val chmode10Sms = """(\w*),FID;024H;L""".r

  val allpasswords = Set("1234", "2121")

  // Set(NOT_SENT, NOT_DELIVERED, DELIVERED, SENT)

  private[this] def usedPassword(text: String): Option[String] = text match {
    case "1234,2121" => Some("1234")
    case "1234,UPW;2121" => Some("1234")
    case autophonePhoneSms(p, t) => Some(p)
    case autophonePhoneSms2(p, t) => Some(p)
    case autophonePhoneSms3(p, t) => Some(p)
    case autophonePhoneSms4(p) => Some(p)
    case chmode10Sms(p) => Some(p)
    case _ => None
  }


  val phoneAnswers = Set(
    "New phone write to sim card. +79857707575",
    "Phone +79857707575 accepted",
    "1: +79857707575", "UNO0:4938",
    "Номер +79857707575 записан"
  )

  val passwAnswers = Set("New password: 2121", "Password 2121 accepted", "UPW:2121", "PASS: 2121")

  private implicit def toctanyof(text: String) = new {
    def containsAnyOf(strings: Traversable[String]): Boolean = strings.exists(s => text.contains(s))

    def containsAnyOf(strings: String*): Boolean = strings.exists(s => text.contains(s))
  }

  private[this] def testAllRight(pairs: Seq[(Seq[MessageInfo], Seq[MessageInfo])]): Boolean = {
    phoneSetBy("2121", pairs) || (phoneSetBy("1234", pairs) && passwSetBy1234(pairs))
  }


  def needToSetPassword(pairs: Seq[(Seq[MessageInfo], Seq[MessageInfo])]) =
    !phoneSetBy("2121", pairs) && phoneSetBy("1234", pairs) && !passwSetBy1234(pairs)


  def passwSetBy1234(pairs: Seq[(Seq[MessageInfo], Seq[MessageInfo])]): Boolean = {
    pairs.exists({
      case (in, out) =>
        in.exists(m => m.getMessageText.containsAnyOf(passwAnswers)) &&
          out.headOption.map(_.getMessageText.containsAnyOf("1234,2121", "1234,p=2121", "1234,UPW;2121")).getOrElse(false)
    })
  }

  def possiblyPasswSetBy1234(pairs: Seq[(Seq[MessageInfo], Seq[MessageInfo])]): Boolean = {
    pairs.exists({
      case (in, out) =>
        in.nonEmpty &&
          out.headOption.map(_.getMessageText.containsAnyOf("1234,2121", "1234,p=2121", "1234,UPW;2121")).getOrElse(false)
    })
  }

  def phoneSetBy(passw: String, pairs: Seq[(Seq[MessageInfo], Seq[MessageInfo])]): Boolean = {
    pairs.exists({
      case (in, out) =>
        in.exists(m => m.getMessageText.containsAnyOf(phoneAnswers)) &&
          out.headOption.map(_.getMessageText.containsAnyOf(phoneChangeCommands(passw))).getOrElse(false)
    })
  }

  def phoneChangeCommands(passw: String) = Seq(passw + ",+79857707575", passw + ",UNO;4938")

  def possiblyPhoneSetBy(passw: String, pairs: Seq[(Seq[MessageInfo], Seq[MessageInfo])]): Boolean = {
    pairs.exists({
      case (in, out) =>
        in.nonEmpty &&
          out.headOption.map(_.getMessageText.containsAnyOf(phoneChangeCommands(passw))).getOrElse(false)
    })
  }

  def allPasswordsTried(allsms: Seq[MessageInfo]) = {
    val delivered = allsms.filter(m => !m.incoming && m.delivered)

    delivered.exists(_.getMessageText.containsAnyOf(phoneChangeCommands("1234"))) &&
      delivered.exists(_.getMessageText.containsAnyOf(phoneChangeCommands("2121")))
  }

  def bindDataToResult(args: Array[String]) {
    val ddata = {
      val pkg = OPCPackage.open("/home/nickl/Загрузки/Копия Спящие отчет-1.xlsx", PackageAccess.READ)
      val workbook = new XSSFWorkbook(pkg);
      val toMap = workbook.getSheet("Лист1").map(r => r.map(_.getTypedValue()).toIndexedSeq).map(a => (a(9), a)).toMap
      pkg.close()
      toMap
    }


    val workbook = new XSSFWorkbook(new FileInputStream("Спящие отчет.xlsx"));

    val sheet = workbook.getSheet("Результаты")
    for (row <- sheet) {

      val phone = "+" + row.getCell(1).getStringCellValue
      ddata.get(phone) match {
        case Some(additional) => {
          val start = row.getLastCellNum
          for ((v, i) <- additional.zipWithIndex) {
            row.createCell(i + start).setTypedValue(v)
          }
        }
        case None =>
      }
    }

    for (i <- 0 to 20) {
      sheet.autoSizeColumn(i)
    }

    val out = new FileOutputStream("Спящие отчет-new.xlsx")
    workbook.write(out)
    out.close()


  }

  def exportResults(args: Array[String]) {
    val workbook = new XSSFWorkbook();
    val sheet = workbook.createSheet("Результаты")

    val cs = workbook.createCellStyle();
    cs.setAlignment(HorizontalAlignment.CENTER)
    val f = workbook.createFont();
    f.setBoldweight(Font.BOLDWEIGHT_BOLD);
    cs.setFont(f);

    val headerRow = sheet.createRow(0)
    headerRow.setRowStyle(cs)

    for ((h, i) <- Seq("Имя", "Номер", "Тип", "Версия", "Результат", "Есть связь", "Новый номер", "Новый пароль").zipWithIndex) {
      headerRow.createCell(i)
      headerRow.getCell(i).setCellValue(h)
    }

    val downloadedSms = sendSms.readSms(allSleepersPhones, new DateTime().minusDays(60).toDate, new Date(), RequestMessageType.ALL)
    val phoneSmses = downloadedSms.getMessageInfo.toSeq.reverse.groupBy(_.getDeviceMsid)
    val data = collection.find().map(dbo => {
      val allsmses = phoneSmses(dbo.as[String]("phone"))
      val pairs = SMSAnalyzeUtils.splitSentResponse(allsmses)
      val r = new ArrayBuffer[Any](7)
      r += dbo.as[String]("imei")
      r += dbo.as[String]("phone")
      r += dbo.as[String]("type")
      r += dbo.as[String]("version")

      val phoneBy2121 = phoneSetBy("2121", pairs)
      val phoneBy1234 = phoneSetBy("1234", pairs)
      val passwby1234 = passwSetBy1234(pairs)

      val allRight = phoneBy2121 || (phoneBy1234 && passwby1234)
      val deliveredAny = allsmses.filter(!_.incoming).exists(_.delivered)
      val hasYetUndelivered = allsmses.takeWhile(!_.incoming).exists(_.deliveryStatus == DeliveryStatus.SENT)

      r += (if (allRight) "OK" else if (hasYetUndelivered) "Ожидает получения" else if (deliveredAny) "Есть ошибки" else "Нет связи")
      r += deliveredAny
      r += (phoneBy2121 || phoneBy1234)
      r += (phoneBy2121 || passwby1234)
      r
    }).toIndexedSeq.sortBy((b: ArrayBuffer[Any]) => b(4).asInstanceOf[AnyRef].toString) /*(Ordering[String].reverse)*/

    for ((rowData, i) <- data.zipWithIndex) {
      val row = sheet.createRow(i + 1)
      for ((v, j) <- rowData.zipWithIndex) {
        row.createCell(j).setTypedValue(v)
      }
    }

    for (i <- 0 to 7) {
      sheet.autoSizeColumn(i)
    }

    val sheet2 = workbook.createSheet("Проблемные диалоги")
    val df = workbook.createDataFormat();
    val dcs = workbook.createCellStyle();
    dcs.setDataFormat(df.getFormat("dd.MM.yy"));
    val row2Count = new AtomicInteger(0)
    for (dbo <- collection.find()) {
      val imei = dbo.as[String]("imei")
      val phone = dbo.as[String]("phone")
      val allsmses = phoneSmses(dbo.as[String]("phone"))
      val pairs = SMSAnalyzeUtils.splitSentResponse(allsmses)
      if (!testAllRight(pairs) && allsmses.filter(!_.incoming).exists(_.delivered)) {
        val r = sheet2.createRow(row2Count.getAndIncrement)
        // r.setRowStyle(cs)
        r.createCell(0).setCellValue(imei)
        r.createCell(1).setCellValue(phone)

        for (sms <- allsmses) {
          val row = sheet2.createRow(row2Count.getAndIncrement)
          row.createCell(2).setCellValue(if (sms.incoming) "<-" else "->")
          row.createCell(3).setCellValue(if (sms.incoming) "" else trDeliveryStatus(sms.deliveryStatus))
          val cellDate = row.createCell(4)
          cellDate.setCellValue(sms.getCreationDate: Date)
          cellDate.setCellStyle(dcs)

          row.createCell(5).setCellValue(sms.getMessageText)
        }
      }
    }

    for (i <- 0 to 5) {
      sheet2.autoSizeColumn(i)
    }

    val out = new FileOutputStream("Спящие отчет.xlsx")
    workbook.write(out)
    out.close()
  }

  def main(args: Array[String]) {

    if (args.contains("real"))
      real = true

    // sendPasswords()
    //sendPhones()
    //changeMode()
    changePhone10()

    //    if (real)
    //      Thread.sleep(1000 * 60)
    //
    //    changeMode10()
  }

  def analize(allsmses: Seq[MessageInfo]) = new {
    val pairs = SMSAnalyzeUtils.splitSentResponse(allsmses)
    val allincoming = allsmses.filter(_.incoming)
    val smsess = new PredicateReader(allsmses)
    val sentButNotAnswered = smsess.takeWhile(!_.incoming)
    val lastIncoming = smsess.takeWhile(_.incoming)
    val activePassword = if (passwSetBy1234(pairs)) Some("2121")
    else
      smsess.remains.filter(m => !m.incoming && m.delivered).headOption.flatMap(m => usedPassword(m.getMessageText))
    val unansweredPasswords = sentButNotAnswered.filter(_.delivered).flatMap(m => usedPassword(m.getMessageText))

  }


  def changePhone10() {
    println("changePhone10 started ")
    val initialMessMap: Map[String, Seq[MessageInfo]] = tendays.getLines().map(a => (a.split("\t")(1), Seq.empty[MessageInfo])).toMap

    val aggr = new ArrayBuffer[String]()
    println("changePhone10 downloading sms ")
    val downloadedSms = sendSms.readSms(initialMessMap.keys.toSeq, new DateTime().minusDays(160).toDate, new Date(), RequestMessageType.ALL)

    for ((phone, allsmses) <- initialMessMap ++ downloadedSms.getMessageInfo.toSeq.reverse.groupBy(_.getDeviceMsid)) {
      val a = analize(allsmses)
      import a._

      if (
        allsmses.count(_.incoming) == 0 &&
          sentButNotAnswered.nonEmpty &&
          !sentButNotAnswered.filter(_.getCreationDate.getTime > (System.currentTimeMillis() - 23 * 60 * 60 * 1000))
            .exists(_.deliveryStatus == DeliveryStatus.SENT) &&
          sentButNotAnswered.count(m =>
            m.deliveryStatus == DeliveryStatus.DELIVERED && m.getMessageText.containsAnyOf("UNO;4938", "+79857707575")
          ) < 3
      ) {
        //      if (/*lastIncoming.length == 0 && */sentButNotAnswered.headOption.map(_.deliveryStatus == DeliveryStatus.NOT_SENT).getOrElse(false)) {
        aggr += phone
        val imei = collection.findOne(MongoDBObject("phone" -> phone)).get.as[String]("imei")
        println(imei + "(" + phone + ") -> passwd=" + activePassword.orNull)
        println("sentButNotAnswered=\n" + sentButNotAnswered.map(m => "* " + "(" + m.deliveryStatus + ") " + m.getMessageText + "\n").mkString(""))
        println("lastIncoming=\n" + lastIncoming.map(m => "* " + m.getMessageText + "\n").mkString(""))
        println("last sent =\n" + smsess.remains.take(5).map(m => "* " + "(" + m.deliveryStatus + ") " + m.getMessageText + "\n").mkString(""))
        val unansweredPasswords = sentButNotAnswered.filter(m => m.delivered && m.getMessageText.contains("UNO;4938"))
          .flatMap(m => usedPassword(m.getMessageText))
        println("unansweredPasswords passwords= " + unansweredPasswords.mkString("[", ",", "]"))
        //val passwToSend = (allpasswords -- unansweredPasswords).headOption.getOrElse("2121")
        val passwToSend = activePassword.filterNot(unansweredPasswords.contains) getOrElse (unansweredPasswords ++ (Seq("1234", "2121", "2121"))).groupBy(a => a).minBy(_._2.length)._1

        val autophone = true
        if (autophone) {
          val smsToSent = passwToSend + ",+79857707575"
          println("smsToSent=" + smsToSent)
          if (real) sendSms.sendSms(phone, smsToSent, forceFederal = true)
        }
        else {
          val smsToSent = passwToSend + ",UNO;4938"
          println("smsToSent=" + smsToSent)
          if (real) sendSms.sendSms(phone, smsToSent)
        }

      }

    }
    println("aggr.count=" + aggr.size)
  }

  def changeMode10() {
    println("changeMode10 started ")
    val initialMessMap: Map[String, Seq[MessageInfo]] = tendays.getLines().map(a => (a.split("\t")(1), Seq.empty[MessageInfo])).toMap

    val aggr = new ArrayBuffer[String]()
    println("changeMode10 downloading sms ")
    val downloadedSms = sendSms.readSms(initialMessMap.keys.toSeq, new DateTime().minusDays(160).toDate, new Date(), RequestMessageType.ALL)

    for ((phone, allsmses) <- initialMessMap ++ downloadedSms.getMessageInfo.toSeq.reverse.groupBy(_.getDeviceMsid)) {
      val a = analize(allsmses)
      import a._

      if (
        allsmses.count(_.incoming) > 0 &&
          !sentButNotAnswered.filter(_.getCreationDate.getTime > (System.currentTimeMillis() - 23 * 60 * 60 * 1000))
            .exists(_.deliveryStatus == DeliveryStatus.SENT) &&
          sentButNotAnswered.count(m =>
            m.deliveryStatus == DeliveryStatus.DELIVERED && m.getMessageText.contains("FID;024H;L")
          ) < 3 &&
          allsmses.filter(_.incoming).exists(m => m.getMessageText.contains("UNO0:4938")) &&
          !allsmses.filter(_.incoming).exists(m => m.getMessageText.contains("FID:024H;L"))
      ) {
        //      if (/*lastIncoming.length == 0 && */sentButNotAnswered.headOption.map(_.deliveryStatus == DeliveryStatus.NOT_SENT).getOrElse(false)) {
        aggr += phone
        val imei = collection.findOne(MongoDBObject("phone" -> phone)).map(_.as[String]("imei")).getOrElse(throw new IllegalArgumentException("no imei on " + phone))
        println(imei + "(" + phone + ") -> passwd=" + activePassword.orNull)
        println("sentButNotAnswered=\n" + sentButNotAnswered.map(m => "* " + "(" + m.deliveryStatus + ") " + m.getMessageText + "\n").mkString(""))
        println("lastIncoming=\n" + lastIncoming.map(m => "* " + m.getMessageText + "\n").mkString(""))
        println("last sent =\n" + smsess.remains.take(5).map(m => "* " + "(" + m.deliveryStatus + ") " + m.getMessageText + "\n").mkString(""))
        println("unansweredPasswords passwords= " + unansweredPasswords.mkString("[", ",", "]"))
        //val passwToSend = (allpasswords -- unansweredPasswords).headOption.getOrElse("2121")
        val passwToSend = activePassword /*.filterNot(unansweredPasswords.contains)*/ getOrElse (unansweredPasswords ++ (Seq("1234", "2121", "2121"))).groupBy(a => a).minBy(_._2.length)._1

        val smsToSent = passwToSend + ",FID;024H;L"
        println("smsToSent=" + smsToSent)
        if (real) sendSms.sendSms(phone, smsToSent)

      }

    }
    println("aggr.count=" + aggr.size)
  }


  def changeMode() {

    val aggr = new ArrayBuffer[String]()
    val downloadedSms = sendSms.readSms(allSleepersPhones, new DateTime().minusDays(160).toDate, new Date(), RequestMessageType.ALL)

    for ((phone, allsmses) <- downloadedSms.getMessageInfo.toSeq.reverse.groupBy(_.getDeviceMsid)) {
      val a = analize(allsmses)
      import a._

      if (
      //  lastIncoming.length > 0 &&
        !sentButNotAnswered.exists(_.deliveryStatus == DeliveryStatus.SENT) &&
          a.lastIncoming.exists(_.getMessageText.contains("Mode=120H"))
      //  testAllRight(pairs)
      ) {
        //      if (/*lastIncoming.length == 0 && */sentButNotAnswered.headOption.map(_.deliveryStatus == DeliveryStatus.NOT_SENT).getOrElse(false)) {
        aggr += phone
        val imei = collection.findOne(MongoDBObject("phone" -> phone)).get.as[String]("imei")
        println(imei + "(" + phone + ") -> passwd=" + activePassword.orNull)
        println("sentButNotAnswered=\n" + sentButNotAnswered.map(m => "* " + "(" + m.deliveryStatus + ") " + m.getMessageText + "\n").mkString(""))
        println("lastIncoming=\n" + lastIncoming.map(m => "* " + m.getMessageText + "\n").mkString(""))

        println("unansweredPasswords passwords= " + unansweredPasswords.mkString("[", ",", "]"))
        //val passwToSend = (allpasswords -- unansweredPasswords).headOption.getOrElse("2121")
        val passwToSend = (if (Set("79160899471").contains(phone)) Some("1234")
        else
          activePassword.filterNot(unansweredPasswords.contains)) getOrElse (unansweredPasswords ++ (Seq("1234", "2121"))).groupBy(a => a).minBy(_._2.length)._1
        println("passwToSent = " + passwToSend)

        //sendSms.sendSms(phone, passwToSend +",024H,F")

      }
      ////            println(phone+"<-"+head.getDeliveryInfo.getDeliveryInfoExt.map(_.getDeliveryStatus).mkString("[",",","]"))
      ////                println(phone+"->"+smses(1).getDeliveryInfo.getDeliveryInfoExt.map(_.getDeliveryStatus).mkString("[",",","]"))
      ////        println(collection.findOne(MongoDBObject("phone" -> phone)).orNull)
    }
    println("aggr.count=" + aggr.size)
  }


  def sendPasswords() {

    val aggr = new ArrayBuffer[String]()
    val downloadedSms = sendSms.readSms(allSleepersPhones, new DateTime().minusDays(160).toDate, new Date(), RequestMessageType.ALL)

    for ((phone, allsmses) <- downloadedSms.getMessageInfo.toSeq.reverse.groupBy(_.getDeviceMsid)) {
      val a = analize(allsmses)
      import a._

      if (
      //  lastIncoming.length > 0 &&
        sentButNotAnswered.count(_.deliveryStatus == DeliveryStatus.NOT_DELIVERED) < 5 &&
          !sentButNotAnswered.exists(_.deliveryStatus == DeliveryStatus.SENT) &&
          !(allPasswordsTried(allsmses) && !activePassword.isDefined) &&

          // !sentButNotAnswered.exists(_.deliveryStatus == DeliveryStatus.DELIVERED) &&
          //     sentButNotAnswered.filter(_.delivered).isEmpty &&
          //    unansweredPasswords.nonEmpty
          //  sentButNotAnswered.forall(!_.delivered)
          //    unansweredPasswords.nonEmpty
          //          lastIncoming.map(_.getMessageText).exists(t =>
          //            t.containsAnyOf("Неправильная команда", "Invalid command")
          //          ) &&

          //       respPassword.map(_ == "1234").getOrElse(false)
          needToSetPassword(pairs) /* && !possiblyPasswSetBy1234(pairs)*/ &&
          !sentButNotAnswered.filter(s => !s.incoming && s.delivered)
            .exists(_.getMessageText.containsAnyOf("1234,UPW;2121", "1234,p=2121", "1234,2121"))
      //  testAllRight(pairs)
      ) {
        //      if (/*lastIncoming.length == 0 && */sentButNotAnswered.headOption.map(_.deliveryStatus == DeliveryStatus.NOT_SENT).getOrElse(false)) {
        aggr += phone
        val imei = collection.findOne(MongoDBObject("phone" -> phone)).get.as[String]("imei")
        println(imei + "(" + phone + ") -> passwd=" + activePassword.orNull)
        println("sentButNotAnswered=\n" + sentButNotAnswered.map(m => "* " + "(" + m.deliveryStatus + ") " + m.getMessageText + "\n").mkString(""))
        println("lastIncoming=\n" + lastIncoming.map(m => "* " + m.getMessageText + "\n").mkString(""))

        println("unansweredPasswords passwords= " + unansweredPasswords.mkString("[", ",", "]"))
        //val passwToSend = (allpasswords -- unansweredPasswords).headOption.getOrElse("2121")
        val passwToSend = activePassword.filterNot(unansweredPasswords.contains) getOrElse (unansweredPasswords ++ (Seq("1234", "2121"))).groupBy(a => a).minBy(_._2.length)._1
        println("passwToSent = " + passwToSend)


        //                val rec = collection.findOne(MongoDBObject("phone" -> phone)).get
        //                rec.as[String]("type") match {
        //                          case "Colubris" => {
        //                            println("sending="+"1234,UPW;2121")
        //                            val r2 = sendSms.sendSms(phone, "1234,UPW;2121")
        //                            println("r2=" + r2)
        //                          }
        //                          case "АвтоФон" if rec.as[String]("version")!="5.6" => {
        //                            println("sending="+"1234,2121")
        //                            val r2 = sendSms.sendSms(phone, "1234,2121", forceFederal = true)
        //                            println("r3=" + r2)
        //                          }
        //                          case "АвтоФон" if rec.as[String]("version")=="5.6" => {
        //                            println("sending="+"1234,p=2121")
        //                            val r2 = sendSms.sendSms(phone, "1234,p=2121", forceFederal = true)
        //                            println("r3=" + r2)
        //                          }
        //                        }
      }
      ////            println(phone+"<-"+head.getDeliveryInfo.getDeliveryInfoExt.map(_.getDeliveryStatus).mkString("[",",","]"))
      ////                println(phone+"->"+smses(1).getDeliveryInfo.getDeliveryInfoExt.map(_.getDeliveryStatus).mkString("[",",","]"))
      ////        println(collection.findOne(MongoDBObject("phone" -> phone)).orNull)
    }
    println("aggr.count=" + aggr.size)
  }

  def sendPhones() {

    val aggr = new ArrayBuffer[String]()
    val downloadedSms = sendSms.readSms(allSleepersPhones, new DateTime().minusDays(160).toDate, new Date(), RequestMessageType.ALL)

    for ((phone, allsmses) <- downloadedSms.getMessageInfo.toSeq.reverse.groupBy(_.getDeviceMsid)) {

      val a = analize(allsmses)
      import a._

      if (
        lastIncoming.length == 0 &&
          !allPasswordsTried(allsmses) &&
          !sentButNotAnswered.exists(_.deliveryStatus == DeliveryStatus.SENT) &&
          !sentButNotAnswered.forall(_.deliveryStatus == DeliveryStatus.NOT_SENT) &&
          sentButNotAnswered.exists(_.deliveryStatus == DeliveryStatus.DELIVERED) &&
          //     sentButNotAnswered.filter(_.delivered).isEmpty &&
          //    unansweredPasswords.nonEmpty
          //  sentButNotAnswered.forall(!_.delivered)
          //       respPassword.map(_ == "1234").getOrElse(false)
          !possiblyPhoneSetBy("2121", pairs) && !possiblyPhoneSetBy("1234", pairs)

      //  testAllRight(pairs)
      ) {
        //      if (/*lastIncoming.length == 0 && */sentButNotAnswered.headOption.map(_.deliveryStatus == DeliveryStatus.NOT_SENT).getOrElse(false)) {
        aggr += phone
        val imei = collection.findOne(MongoDBObject("phone" -> phone)).get.as[String]("imei")
        println(imei + "(" + phone + ") -> passwd=" + activePassword.orNull)
        println("sentButNotAnswered=\n" + sentButNotAnswered.map(m => "* " + "(" + m.deliveryStatus + ") " + m.getMessageText + "\n").mkString(""))
        println("lastIncoming=\n" + lastIncoming.map(m => "* " + m.getMessageText + "\n").mkString(""))

        //printHistory(pairs)

        println("unansweredPasswords passwords= " + unansweredPasswords.mkString("[", ",", "]"))
        //val passwToSend = (allpasswords -- unansweredPasswords).headOption.getOrElse("2121")
        val passwToSend = (unansweredPasswords ++ (Seq("1234", "2121"))).groupBy(a => a).minBy(_._2.length)._1
        println("passwToSent = " + passwToSend)


        //                collection.findOne(MongoDBObject("phone" -> phone)).get.as[String]("type") match {
        //                  case "Colubris" => {
        //                    //val r2 = sendSms.sendSms(phone, sentButNotAnswered.head.getMessageText)
        //                    //val r2 = sendSms.sendSms(phone, "1234,UPW;2121")
        //                    val r2 = sendSms.sendSms(phone, passwToSend + ",UNO;4938")
        //                    println("r2=" + r2)
        //                  }
        //                  case "АвтоФон" => {
        //                    //val r2 = sendSms.sendSms(phone, sentButNotAnswered.head.getMessageText, forceFederal = true)
        //                    //val r2 = sendSms.sendSms(phone, "1234,2121", forceFederal = true)
        //                    val r2 = sendSms.sendSms(phone, passwToSend + ",+79857707575", forceFederal = true)
        //                    println("r3=" + r2)
        //                  }
        //                }
      }
      ////            println(phone+"<-"+head.getDeliveryInfo.getDeliveryInfoExt.map(_.getDeliveryStatus).mkString("[",",","]"))
      ////                println(phone+"->"+smses(1).getDeliveryInfo.getDeliveryInfoExt.map(_.getDeliveryStatus).mkString("[",",","]"))
      ////        println(collection.findOne(MongoDBObject("phone" -> phone)).orNull)
    }
    println("aggr.count=" + aggr.size)
  }


  def printHistory(sentResponses: Seq[(Seq[MessageInfo], Seq[MessageInfo])]) {
    for ((in, out) <- sentResponses) {
      println("in=\n" + in.map(m => "* " + "(" + m.deliveryStatus + ") " + m.getMessageText + "\n").mkString(""))
      println("out=\n" + out.map(m => "* " + "(" + m.deliveryStatus + ") " + m.getMessageText + "\n").mkString(""))
    }
  }

  lazy val allSleepersPhones = {
    (for (x <- collection.find()) yield {
      x.as[String]("phone")
    }).toSeq
  }


  def main0(args: Array[String]) {

    //    val p = parseTable(autopohones56.getLines()) ++  parseTable(autophones44.getLines())  ++  parseTable(autophone31.getLines()) ++ parseTable(colibrises.getLines())
    //
    //    println("p="+p.size)
    //
    ////        collection.ensureIndex(MongoDBObject("imei" -> 1), MongoDBObject("unique" -> 1))
    ////    collection.ensureIndex(MongoDBObject("phone" -> 1), MongoDBObject("unique" -> 1))
    //
    //    p.foreach(println)

    //    for (e <- p) {
    //      collection.insert(e)
    //    }

    //        for (e <- collection.find(MongoDBObject("type" -> "АвтоФон"))) {
    //
    //          //6665
    //
    //          try {
    //            println("adding:" + e.as[String]("phone"))
    //            val res = sendSms.addUser(e.as[String]("imei"), e.as[String]("phone"), null, 6665)
    //            println("res:" + res)
    //          }
    //          catch {
    //            case e: Exception => e.printStackTrace()
    //          }
    //
    //        }


  }


  def parseTable(data: Iterator[String]): Seq[Map[String, String]] = {
    val p = data.map(_.split("\t").toIndexedSeq).filter(_.length == 4).map(a =>
      Map("type" -> a(0), "version" -> a(1), "imei" -> a(2), "phone" -> a(3).stripPrefix("+")))
    p.toSeq
  }

  def trDeliveryStatus(ds: Option[DeliveryStatus]) = {
    ds match {
      case Some(ds) => ds match {
        case DeliveryStatus.DELIVERED => "Доставлено"
        case DeliveryStatus.NOT_DELIVERED => "Не доставлено"
        case DeliveryStatus.NOT_SENT => "Не отправлено"
        case DeliveryStatus.PENDING => "Ожидает отправки"
        case DeliveryStatus.SENDING => "Отправляется"
        case DeliveryStatus.SENT => "Отправлено"
      }
      case None => "Не известно"
    }
  }

}

