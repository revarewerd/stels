package ru.sosgps.wayrecall.yandexMock

import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import collection.JavaConversions.mapAsScalaMap

/**
 * Created by IVAN on 16.06.2016.
 */

@Component
class PaymentFormServlet extends HttpServlet with grizzled.slf4j.Logging {

  @Autowired
  var context: ApplicationContext = null

  override def doPost(request: HttpServletRequest, response: HttpServletResponse) {
    debug("PaymentFormServlet Servlet POST request")
    val shopParams = context.getBean("shopParams").asInstanceOf[ShopParams]
    val paramMap = request.getParameterMap.mapValues(_.head).toMap
    debug("paramMap = " + paramMap)
    val visibleFields = List("sum", "customerNumber")
    val fieldsTranslationRu = Map("sum" -> "Сумма", "customerNumber" -> "ID Клиента")
    val inputs = paramMap.map(entry =>
      if (!visibleFields.contains(entry._1)) "<tr><input name=\"" + entry._1 + "\" value=\"" + entry._2 + "\"type=\"hidden\"/></tr>"
      else "<tr><td><label>" + fieldsTranslationRu(entry._1) + "</label></td>" +
        "<td><input name=\"" + entry._1 + "\" value=\"" + entry._2 + "\"/></td></tr>"
    ).reduceLeft((a, b) => a + b)
    val result = "<head> <title>Payment Mock Service</title></head><body><h1>Payment Mock Service</h1><br>" +
      "<form action=\"" + "https://" + shopParams.yandexMockUrl+ "/processPayment" + "\" method=\"post\"><table>" +
      inputs + "</table><p><input type=\"submit\" value=\"Заплатить\"/></p></form></body>"
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("text/html");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().println(result);
  }
}

