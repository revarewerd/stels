package ru.sosgps.wayrecall.security

import javax.servlet.http.HttpServletResponse

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, ResponseBody}

/**
 * Created by IVAN on 29.03.2016.
 */
@Controller
class DealerBlockingServlet extends grizzled.slf4j.Logging {


  @RequestMapping(value = Array("/banned"),produces = Array("text/html;charset=UTF-8"))
  @ResponseBody
  def banned(@RequestParam("id") id: String,@RequestParam("balance") balance:String, @RequestParam("service") service:String): String = {
    service match {
      case "billing" =>
        "<head></head>" +
          "<body>" +
          "<h1>" +
          "Портал '" + id + "' заблокирован</br>" +
          "Остаток средств на счете: " + balance + "руб."+
          "</h1>" +
          "</body>"
      case _ =>
        "<head></head>" +
          "<body>" +
          "<h1>" +
          "По техническим причинам, сервис '" + id + "' временно недоступен</br>" +
          "Приносим извинения за предоставленные неудобства" +
          "</h1>" +
          "</body>"
    }
  }
}
