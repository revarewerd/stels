package ru.sosgps.wayrecall.billing

import java.io.EOFException
import java.util.Date
import java.util.zip.{ZipException, GZIPInputStream, GZIPOutputStream}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectFormPostResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, RequestMapping}
import org.springframework.web.multipart.MultipartFile
import ru.sosgps.wayrecall.core.GPSDataConversions
import ru.sosgps.wayrecall.data.{DBPacketsWriter, PackagesStore}
import ru.sosgps.wayrecall.utils.errors.NotPermitted
import ru.sosgps.wayrecall.utils.io.{DboReader, DboWriter}
import ru.sosgps.wayrecall.utils.{ExtDirectService, parseDate}

import scala.beans.BeanProperty


@Controller
@ExtDirectService
@RequestMapping(Array("/dataexport"))
class GPSDataExport extends grizzled.slf4j.Logging{

  @Autowired
  var packStore: PackagesStore = null

  @Autowired
  var store: PackagesStore = null

  @Autowired
  @BeanProperty
  var dbwriter: DBPacketsWriter = null

  @RequestMapping(Array("/download"))
  def download(
                 //FIXME: так и не cмог настроить конвертеры в спринг чтобы даты можно было читать из целых чисел
                 //                 @RequestParam("uid") uid: String,
                 //                 @RequestParam("from") from: Date,
                 //                 @RequestParam("to") to: Date,
                 request: HttpServletRequest,
                 response: HttpServletResponse) {

    val uid = Option(request.getParameter("uid")).get
    val from = Option(request.getParameter("from")).map(parseDate).get
    val to = Option(request.getParameter("to")).map(parseDate).get
    response.setContentType("application/octet-stream")
    response.setHeader("Content-Disposition", "attachment; filename=\"" + uid + ".bson.gz\"");
    val out = response.getOutputStream
    //    out.println("uid:"+uid)
    //    out.println("from:"+from)
    //    out.println("to:"+to)

    val writer = new DboWriter(new GZIPOutputStream(out))

    for (e <- store.getHistoryFor(uid, from, to)) {
      writer.write(GPSDataConversions.toMongoDbObject(e))
    }
    writer.close()


  }

  @ExtDirectMethod(ExtDirectMethodType.FORM_POST)
  @RequestMapping(Array("/upload"))
  def uploadFile(@RequestParam("datafile") dataFile: MultipartFile,
                 @RequestParam("datatype") datatype: String,
                 @RequestParam("uid") uid: String
                  ): ExtDirectFormPostResult = {

    debug("datafile:"+dataFile+" datatype:"+datatype+" uid:"+uid)
    var result=new ExtDirectFormPostResult();
    val inputStream = dataFile.getInputStream
    try {
      val reader = new DboReader(new GZIPInputStream(inputStream))

      for (e <- reader.iterator) {
        val gps = GPSDataConversions.fromDbo(e)
        gps.uid = uid
        dbwriter.addToDb(gps)
      }

    }
    catch
      {
        case e: NotPermitted => {
            result.setSuccess(false)
            result.addError("error",e.getMessage)
        }
        case  e @ (_ : EOFException | _ : ZipException)   => {
            result.setSuccess(false)
            result.addError("error","Некорректный тип файла")
        }
      }
    finally {
      inputStream.close()
      store.refresh(uid)
   }
   result
  }

}
