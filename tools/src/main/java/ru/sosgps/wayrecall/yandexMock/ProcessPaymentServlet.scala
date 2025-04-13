package ru.sosgps.wayrecall.yandexMock

import java.util.concurrent.ExecutorService
import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import collection.JavaConversions.mapAsScalaMap

/**
 * Created by IVAN on 12.10.2016.
 */

@Component
class ProcessPaymentServlet extends HttpServlet with grizzled.slf4j.Logging {

  @Autowired
  var paymentBus : ExecutorService = null

  @Autowired
  var paymentProcessFactory : PaymentProcessFactory = null

  @Autowired
  var context: ApplicationContext = null

  override def doPost(request: HttpServletRequest, response: HttpServletResponse)  {
    debug("ProcessPaymentServlet POST request")
    val shopParams = context.getBean("shopParams").asInstanceOf[ShopParams]
    val paramMap=request.getParameterMap.mapValues(_.head).toMap
    debug("paramMap = " + paramMap)
    val result =
      try {
        val paymentProcess = paymentProcessFactory.newPaymentProcess(paramMap)
        paymentBus.execute(paymentProcess)
        response.setStatus(HttpServletResponse.SC_OK);
        "acceptPayment"
      }
      catch {
        case e: Exception => {
          debug("PaymentFormServlet catch error: " + e.getMessage)
          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          e.getMessage
        }
      }
    val page="<head> <title>Payment Mock Service</title></head>" +
      "<body>" +
      "<h1>Payment Mock Service</h1><br" +
      (result match {
        case "acceptPayment" => "<p>Платеж принят к обработке</p>"
        case e => "<p>Во время платежа произошла ошибка: "+e+"</p>" }) +
      "<a href=\""+shopParams.shopUrl+"\">Вернуться в систему Wayrecall</a></body>"

    response.setContentType("text/html");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().println(page);
  }
}
