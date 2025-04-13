package ru.sosgps.wayrecall.payment

import scala.beans.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.{Component, Controller}
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod}
import com.mongodb.casbah.commons.MongoDBObject
import org.apache.commons.codec.digest.DigestUtils
import org.bson.types.ObjectId
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import ru.sosgps.wayrecall.data.UserMessage
import ru.sosgps.wayrecall.events.EventsStore
import collection.JavaConversions.mapAsScalaMap
import scala.xml.{Null, Text, Attribute, Elem}

import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.core.finance.FeeProcessor

/**
 * Created by IVAN on 14.09.2016.
 */
@Controller
class PaymentServlet extends grizzled.slf4j.Logging {

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var es: EventsStore = null

  @Autowired
  var feeProcessor: FeeProcessor = null

  @Autowired
  var paymentParams: PaymentParams = null

  @RequestMapping(Array("/checkOrder"))
  def checkOrder(request: HttpServletRequest,
                 response: HttpServletResponse) = {
    val checkOrderResult = checkOrderRequestProcessing(request.getParameterMap.mapValues(_.head).toMap)
    response.setContentType("application/xml");
    response.setCharacterEncoding("UTF-8");
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().print(checkOrderResult)
  }

  def checkOrderRequestProcessing(paramMap: Map[String, String]) = {
    debug("CheckOrder request params=" + paramMap)
    val processingResult = requestProcessing(List("checkOrder"), paramMap)
    val resultDBO = MongoDBObject(
      "action" -> "checkOrder",
      "invoiceId" -> paramMap.getOrElse("invoiceId", ""),
      "request" -> paramMap,
      "result" -> processingResult.toMap
    )
    mdbm.getDatabase()("yandexPayment").insert(resultDBO)
    createXmlDocument("checkOrder", processingResult)
  }

  @RequestMapping(Array("/processOrder"))
  def processOrder(request: HttpServletRequest,
                   response: HttpServletResponse) = {
    val processOrderResult = processOrderRequestProcessing(request.getParameterMap.mapValues(_.head).toMap)
    response.setContentType("application/xml");
    response.setCharacterEncoding("UTF-8");
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter.print(processOrderResult)
  }

  def processOrderRequestProcessing(paramMap: Map[String, String]): Elem = {
    debug("ProcessOrder request params =" + paramMap)
    val processingResult = requestProcessing(List("paymentAviso", "cancelOrder"), paramMap)
    val action = paramMap.getOrElse("action", "")
    debug("action =" + action)
    val result: PaymentResponse = processingResult match {
      case sr:SuccessPaymentResponse if (action == "paymentAviso") => {
        val checkOrder = mdbm.getDatabase()("yandexPayment").findOne(MongoDBObject("invoiceId" -> sr.invoiceId, "action" -> "checkOrder"))
        checkOrder match {
          case Some(_) => {
            val paymentAviso = mdbm.getDatabase()("yandexPayment").findOne(MongoDBObject("invoiceId" -> sr.invoiceId, "action" -> "paymentAviso"))
            paymentAviso match {
              case Some(_) => {
                debug("Платеж уже был зачислен, отправлен повторный ответ об успешном платеже")
                sr
              }
              case None => {
                successPayment(paramMap("customerNumber"), sr)
                debug("Произведено зачисление платежа")
                sr
              }
            }
          }
          case None => {
            debug("Заказ не прошел проверку.Отсутствует информация о прохождении проверки \"checkOrder\" для заказа с invoiceId=" + sr.invoiceId)
            ErrorPaymentResponse("100", sr.performedDatetime,
              "Заказ не прошел проверку", "Отсутствует информация о прохождении проверки \"checkOrder\" для заказа с invoiceId=" + sr.invoiceId)
          }
        }
      }
      case response => response
    }
    val resultDBO = MongoDBObject(
      "action" -> action,
      "invoiceId" -> paramMap.getOrElse("invoiceId", ""),
      "request" -> paramMap,
      "result" -> result.toMap
    )
    mdbm.getDatabase()("yandexPayment").insert(resultDBO)
    createXmlDocument(action,result)
  }

  def successPayment(account: String, successResponse: SuccessPaymentResponse): Unit = {
    debug("feeProcessor" + feeProcessor)
    val amount100=successResponse.shopSumAmount
    val paymentTypes = Map("PC" -> "Yandex деньги","AC"->"Банковская карта", "WM"->"WebMoney", "SB"->"Сбербанк Онлайн","QW"->"QIWI Wallet")
    var details = "Номер транзакции: "+successResponse.invoiceId+"; сумма платежа: "+successResponse.orderSumAmount+
      " р.; сумма с учетом комиссии: "+successResponse.shopSumAmount+" р.; тип платежа: "+paymentTypes(successResponse.paymentType)
    if(successResponse.paymentType=="PC") details+="; номер счета в Яндекс.Деньгах: "+successResponse.paymentPayerCode

    feeProcessor.changeBalance(new ObjectId(account), (amount100.toDouble * 100).asInstanceOf[Long], "Yandex касса",details)
    val userNames = mdbm.getDatabase()("users").find(
      MongoDBObject("mainAccId" -> new ObjectId(account), "showbalance" -> true), MongoDBObject("name" -> "1")
    ).map(_.get("name").asInstanceOf[String])
    userNames.foreach(name => es.publish(new UserMessage(name, "Зачислено рублей: " + amount100, "Пополнение счета", None, None)))
  }

  def requestProcessing(actions: List[String], paramMap: Map[String, String]): PaymentResponse = {
    val currentShopId = paymentParams.shopId
    val shopPassword = paymentParams.shopPassword
    val dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
    val performedDatetime = dateTimeFormatter.print(new DateTime())

    //Параметры для MD5 обязательно должны присутствовать:
    //  action, orderSumAmount, orderSumCurrencyPaycash, orderSumBankPaycash, shopId, invoiceId, customerNumber, shopPassword, md5
    //Остальные параметры:
    //  requestDatetime,shopArticleId,orderNumber,orderCreatedDatetime,
    //  shopSumAmount,shopSumCurrencyPaycash,shopSumBankPaycash, paymentPayerCode,paymentType,comment

    val action = paramMap.getOrElse("action", "")
    if (!actions.contains(action))
      return ErrorPaymentResponse("100",performedDatetime,
        "Некорректный тип запроса - 'action'","Тип запроса должен быть " + actions.reduceLeft(_ + " или " + _))

    val orderSumAmount = paramMap.getOrElse("orderSumAmount", "0")
    if (orderSumAmount.toDouble <= 0)
      return ErrorPaymentResponse("100",performedDatetime,
        "Некорректная сумма заказа  - 'orderSumAmount'","Сумма заказа не может быть отрицательно либо равной 0")

    val orderSumCurrencyPaycash = paramMap.getOrElse("orderSumCurrencyPaycash", "")
    if (orderSumCurrencyPaycash != "643" && orderSumCurrencyPaycash != "10643")
      return ErrorPaymentResponse("100",performedDatetime,
        "Некорректный код валюты суммы заказа - 'orderSumCurrencyPaycash'","Код валюты суммы заказа должен быть = 643 (Рубль РФ)")

    val orderSumBankPaycash = paramMap.getOrElse("orderSumBankPaycash", "")
    if (orderSumBankPaycash.isEmpty)
      return ErrorPaymentResponse("100",performedDatetime,
        "Некорректный код процессингового центра - 'orderSumBankPaycash'","Не указан код процессингового центра")

    val shopId = paramMap.getOrElse("shopId", "")
    if (currentShopId != shopId)
      return ErrorPaymentResponse("100",performedDatetime,
        "Некорректный идентификатор магазина - 'shopId'","Идентификатор магазина не совпадает с указанным в системе")

    val invoiceId = paramMap.getOrElse("invoiceId", "")
    if (invoiceId.isEmpty)
      return ErrorPaymentResponse("100",performedDatetime,
        "Некорректный уникальный номер транзакции - 'invoiceId'","Не указан уникальный номер транзакции")

    val customerNumber = paramMap.getOrElse("customerNumber", "")
    val account = if (customerNumber.nonEmpty)
      mdbm.getDatabase()("accounts").findOne(MongoDBObject("_id" -> new ObjectId(customerNumber)))
    else None
    if (account.isEmpty)
      return ErrorPaymentResponse("100",performedDatetime,
        "Некорректный идентификатор плательщика - 'customerNumber'","Пользователь с указанным идентификатором не существует")

    val md5 = paramMap.getOrElse("md5", "")
    val md5Params = List(action, orderSumAmount, orderSumCurrencyPaycash, orderSumBankPaycash, shopId, invoiceId, customerNumber, shopPassword).reduceLeft(_ + ";" + _)
    val md5Check = DigestUtils.md5Hex(md5Params).toUpperCase;
    if (md5Check != md5) return ErrorPaymentResponse("1",performedDatetime,"Несовпадение значения параметра md5 с результатом расчета хэш-функции")

    val shopSumAmount = paramMap.getOrElse("shopSumAmount", "")
    if (shopSumAmount.toDouble <= 0)
      return ErrorPaymentResponse("1",performedDatetime,
        "Некорректная сумма к выплате на расчетный счет   - 'shopSumAmount'","Сумма заказа не может быть отрицательно либо равной 0")

    val shopSumCurrencyPaycash = paramMap.getOrElse("shopSumCurrencyPaycash", "")
    if (shopSumCurrencyPaycash != "643" && shopSumCurrencyPaycash != "10643")
      return ErrorPaymentResponse("1",performedDatetime,
        "Некорректный код валюты суммы к выплате на расчетный счет - 'shopSumCurrencyPaycash'","Код валюты суммы заказа должен быть = 643 (Рубль РФ)")

    val shopSumBankPaycash = paramMap.getOrElse("shopSumBankPaycash", "")
    if (shopSumBankPaycash.isEmpty)
      return ErrorPaymentResponse("1",performedDatetime,
        "Некорректный код процессингового центра - 'shopSumBankPaycash'","Не указан код процессингового центра")

    val paymentType = paramMap.getOrElse("paymentType", "")
    val paymentTypes = List("PC", "AC", "MC", "GP", "WM", "SB", "MP", "AB", "MA", "PB", "QW", "KV", "QP")
    if (!paymentTypes.contains(paymentType)) return ErrorPaymentResponse("1",performedDatetime,
      "Некорректный способ оплаты заказа - 'paymentType'","Способ оплаты заказа не совпадает ни с одним из возможных кодов способов оплаты")

    val paymentPayerCode = paramMap.getOrElse("paymentPayerCode", "")
    if (paymentType == "PC" && paymentPayerCode.isEmpty)
       return ErrorPaymentResponse("1",performedDatetime,
        "Некоретный номер кошелька  в Яндекс.Деньгах","Не указан номер колеька в Яндекс.Деньгах")

    SuccessPaymentResponse("0",shopId,invoiceId,orderSumAmount,shopSumAmount,performedDatetime,paymentType,paymentPayerCode)
  }

  def createXmlDocument(action: String, data:PaymentResponse/*data: Map[String, String]*/) = {
    val initElem = scala.xml.XML.loadString("<" + action + "Response" + "/>")
    data.toMap.foldLeft(initElem)((prev, next) => prev % Attribute(None, next._1, Text(next._2), Null))
  }
}

abstract class PaymentResponse {
  def toMap: Map[String, String]
}

case class SuccessPaymentResponse(val code: String, val shopId: String, val invoiceId: String, val orderSumAmount: String,
                                  val shopSumAmount:String, val performedDatetime: String,val paymentType: String,val paymentPayerCode: String)
  extends PaymentResponse {
  def toMap = Map("code" -> code, "shopId" -> shopId, "invoiceId" -> invoiceId, "orderSumAmount" -> orderSumAmount,
    "shopSumAmount"->shopSumAmount,"performedDatetime" -> performedDatetime)
}

case class ErrorPaymentResponse(val code: String, val performedDatetime: String, val message: String, val techMessage: String = "")
  extends PaymentResponse {
  def toMap = Map("code" -> code, "performedDatetime" -> performedDatetime, "message" -> message, "techMessage" -> techMessage)
}

class PaymentParams(val shopPassword: String, val shopId: String,  val scId: String,val paymentServerUrl:String)

