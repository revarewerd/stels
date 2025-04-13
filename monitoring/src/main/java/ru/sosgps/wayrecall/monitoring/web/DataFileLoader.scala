package ru.sosgps.wayrecall.monitoring.web

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectFormPostResult
import java.io.{Reader, InputStreamReader}
import com.googlecode.jcsv.reader.internal.CSVReaderBuilder
import java.io.IOException
import scala.collection.JavaConverters.asScalaBufferConverter

import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import ru.sosgps.wayrecall.utils.ExtDirectService


@ExtDirectService
class DataFileLoader extends grizzled.slf4j.Logging {
  
  @ExtDirectMethod(ExtDirectMethodType.FORM_POST)
  def getUploadedFileData(@RequestParam("dataFile") dataFile: MultipartFile): ExtDirectFormPostResult = {
    debug("File name = " + dataFile.getName + ", file original name = " + dataFile.getOriginalFilename + ", file type = " + dataFile.getContentType)
    val resp = new ExtDirectFormPostResult()
    
    if (!dataFile.isEmpty()) {
      val csvFile = new InputStreamReader(dataFile.getInputStream)
      val csvReader = CSVReaderBuilder.newDefaultReader(csvFile)
      val lines = csvReader.readAll
      val map = lines.asScala.map(l => {
        Map(
          "x" -> {if (l(0).startsWith("\uFEFF")) l(0).substring(1) else l(0)},
          "y" -> l(1)
        )
      })
      debug("Data table map = " + map)
      resp.addResultProperty("data", map)
    } else {
      resp.addResultProperty("error", "Файл имеет нулевой размер")
    }
    resp
  }
}
