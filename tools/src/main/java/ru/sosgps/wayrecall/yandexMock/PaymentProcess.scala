package ru.sosgps.wayrecall.yandexMock

import java.util.concurrent.{TimeUnit}
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.{PreDestroy, PostConstruct}
import com.mongodb.casbah.WriteConcern
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._
import org.apache.commons.codec.digest.DigestUtils
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.{ContentResponse,Request}
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import ru.sosgps.wayrecall.core.MongoDBManager
import scala.util.Random

import scala.collection.JavaConversions.asScalaIterator

/**
 * Created by IVAN on 17.06.2016.
 */

@Component
class PaymentProcessFactory extends grizzled.slf4j.Logging {

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var context: ApplicationContext = null

  val httpClient: HttpClient = new HttpClient(new SslContextFactory(true))

  var invoiceCount: AtomicLong = null

  @PostConstruct
  def init() ={
    invoiceCount = new AtomicLong(mdbm.getDatabase()("paymentStat").findOne(MongoDBObject("_id"->1))
      .map(_.get("invoiceCount").asInstanceOf[Long]).getOrElse(0L))
    httpClient.start()
    debug("HttpClient started")
  }

  @PreDestroy
  def destroy() = {
    httpClient.stop()
    debug("HttpClient stoped")
  }

  def newPaymentProcess(paramMap: Map[String, String]): PaymentProcess = {
    val shopParams=context.getBean("shopParams").asInstanceOf[ShopParams]
    val currentInvoice = invoiceCount.incrementAndGet()
    mdbm.getDatabase()("paymentStat")
      .update(MongoDBObject("_id"->1),$set("invoiceCount"->currentInvoice),true,true,WriteConcern.Safe)
    new PaymentProcess(currentInvoice,paramMap, mdbm, httpClient, shopParams)
  }
}

class EmptyParamException(val mes: String) extends NoSuchElementException(mes)

class PaymentProcess(invoiceId: Long,paramMap: Map[String, String], mdbm: MongoDBManager, httpClient: HttpClient,shopParams: ShopParams) extends Runnable with grizzled.slf4j.Logging {
  debug("New payment process invoiceId=" + invoiceId)

  def nonEmptyParam(paramName: String) = paramMap.get(paramName).filter(_.nonEmpty)
    .getOrElse(throw new EmptyParamException(paramName + " undefined"))

  val shopId = nonEmptyParam("shopId")
  val scid = nonEmptyParam("scId")
  val orderSumAmount = nonEmptyParam("sum")
  val customerNumber = nonEmptyParam("customerNumber")
  val paymentType = nonEmptyParam("paymentType")

  val shopArticleId = paramMap.getOrElse("shopArticleId", "")
  val comment = paramMap.getOrElse("comment", "")
  val dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
  val requestDatetime = dateTimeFormatter.print(new DateTime())
  val cps_email=paramMap.getOrElse("cps_email", "")
  val cps_phone=paramMap.getOrElse("cps_phone", "")
  debug("requestDatetime " + requestDatetime)

  var orderCreatedDateTime: String = null
  var paymentDatetime: String = null

  val paymentProcessDBO = MongoDBObject(
    "invoiceId" -> invoiceId,
    "requestDatetime" -> requestDatetime,
    "shopId" -> shopId,
    "shopArticleId" -> shopArticleId,
    "scid" -> scid,
    "orderSumAmount" -> orderSumAmount,
    "customerNumber" -> customerNumber,
    "paymentType" -> paymentType,
    "cps_email"->cps_email,
    "cps_phone"->cps_phone,
    "comment" -> comment
  )

  mdbm.getDatabase()("paymentProcess").insert(paymentProcessDBO,WriteConcern.Safe)

  def prepareCheckOrderRequest() = {
    orderCreatedDateTime = dateTimeFormatter.print(new DateTime())
    val action = "checkOrder"
    val orderSumCurrencyPaycash=10643.toString
    val orderSumBankPaycash=1003.toString
    val shopSumCurrencyPaycash=10643.toString
    val shopSumBankPaycash=1003.toString
    val shopPassword=shopParams.shopPassword
    val paymentPayerCode="12345678901"
    val md5Params=List(action, orderSumAmount, orderSumCurrencyPaycash, orderSumBankPaycash, shopId, invoiceId.toString, customerNumber, shopPassword).reduceLeft(_ +";"+ _)
    val md5=DigestUtils.md5Hex(md5Params).toUpperCase;
    val checkOrderUrl="https://"+shopParams.paymentUrl+"/checkOrder"
    httpClient.POST(checkOrderUrl)
      .param("requestDatetime",requestDatetime)
      .param("action",action)
      .param("md5",md5)
      .param("shopId",shopId)
      .param("shopArticleId",shopArticleId)
      .param("invoiceId",invoiceId.toString)
      .param("orderNumber","")
      .param("customerNumber",customerNumber)
      .param("orderCreatedDatetime",orderCreatedDateTime)
      .param("orderSumAmount",orderSumAmount)
      .param("orderSumCurrencyPaycash", orderSumCurrencyPaycash)
      .param("orderSumBankPaycash", orderSumBankPaycash)
      .param("shopSumAmount", orderSumAmount)
      .param("shopSumCurrencyPaycash", shopSumCurrencyPaycash)
      .param("shopSumBankPaycash", shopSumBankPaycash)
      .param("paymentPayerCode", paymentPayerCode)
      .param("paymentType", paymentType)
      .param("comment", comment)
      .timeout(10, TimeUnit.SECONDS)
  }

  def prepareProcessOrderRequest(action:String) = {
    paymentDatetime = dateTimeFormatter.print(new DateTime())
    val orderSumCurrencyPaycash=10643.toString
    val orderSumBankPaycash=1003.toString
    val shopSumCurrencyPaycash=10643.toString
    val shopSumBankPaycash=1003.toString
    val shopPassword=shopParams.shopPassword
    val paymentPayerCode="12345678901"
    val md5Params=List(action, orderSumAmount, orderSumCurrencyPaycash, orderSumBankPaycash, shopId, invoiceId.toString, customerNumber, shopPassword).reduceLeft(_ +";"+ _)
    val md5=DigestUtils.md5Hex(md5Params).toUpperCase;
    val processOrderUrl="https://"+shopParams.paymentUrl+"/processOrder"
    httpClient.POST(processOrderUrl)
      .param("requestDatetime",requestDatetime)
      .param("paymentDatetime",paymentDatetime)
      .param("action",action)
      .param("md5",md5)
      .param("shopId",shopId)
      .param("shopArticleId",shopArticleId)
      .param("invoiceId",invoiceId.toString)
      .param("orderNumber","")
      .param("customerNumber",customerNumber)
      .param("orderCreatedDatetime",orderCreatedDateTime)
      .param("orderSumAmount",orderSumAmount)
      .param("orderSumCurrencyPaycash", orderSumCurrencyPaycash)
      .param("orderSumBankPaycash", orderSumBankPaycash)
      .param("shopSumAmount", orderSumAmount)
      .param("shopSumCurrencyPaycash", shopSumCurrencyPaycash)
      .param("shopSumBankPaycash", shopSumBankPaycash)
      .param("paymentPayerCode", paymentPayerCode)
      .param("paymentType", paymentType)
      .param("cps_user_country_code", "RU")
      .param("comment", comment)
      .timeout(10, TimeUnit.SECONDS)
  }

  def parseCheckOrderResponse(checkOrderResponse: ContentResponse): Map[String, String] = {
    debug("checkOrderResponse xml= "+checkOrderResponse.getContentAsString)
    def nonEmptyParam(param: String) = if(param.nonEmpty) param else throw  new EmptyParamException(param + " undefined")
    val checkOrderXml = scala.xml.XML.loadString(checkOrderResponse.getContentAsString)
    Map(
      "action" -> nonEmptyParam(checkOrderXml.label),
      "code" ->nonEmptyParam((checkOrderXml \ "@code").text),
      "performedDatetime" -> nonEmptyParam((checkOrderXml \ "@performedDatetime").text),
      "shopId" -> (checkOrderXml \ "@shopId").text,
      "invoiceId" -> (checkOrderXml \"@invoiceId").text,
      "orderSumAmount" -> (checkOrderXml \ "@orderSumAmount").text,
      "message" -> (checkOrderXml \ "@message").text,
      "techMessage" -> (checkOrderXml  \ "@techMessage").text
    ) ++ paramMap
  }

  def parseProcessOrderResponse(processOrderResponse: ContentResponse): Map[String, String] = {
    debug("processOrderResponse xml= "+processOrderResponse.getContentAsString)
    def nonEmptyParam(param: String) = if(param.nonEmpty) param else throw  new EmptyParamException(param + " undefined")
    val processOrderXml = scala.xml.XML.loadString(processOrderResponse.getContentAsString)
    Map(
      "action" -> nonEmptyParam(processOrderXml.label),
      "code" ->nonEmptyParam((processOrderXml \ "@code").text),
      "performedDatetime" -> nonEmptyParam((processOrderXml \ "@performedDatetime").text),
      "shopId" ->(processOrderXml \ "@shopId").text,
      "invoiceId" -> (processOrderXml \"@invoiceId").text,
      "orderSumAmount" -> (processOrderXml \ "@orderSumAmount").text,
      "message" -> (processOrderXml \ "@message").text,
      "techMessage" -> (processOrderXml  \ "@techMessage").text
    ) ++ paramMap
  }

  def requestParamsAsMap(request: Request): Map[String,String] = {
    request.getParams.iterator().map(field=> {
      (field.getName,field.getValue)
    }).toMap
  }

  def performCheckOrder(): Map[String, Object] = {
    var checkOrderResponse: ContentResponse = null
    var checkOrderResponseMap: Map[String, Object] = Map.empty
    val checkOrderRequest=prepareCheckOrderRequest()
    try {
      checkOrderResponse = checkOrderRequest.send()
    }
    catch {
      case e => {
        error(e.getMessage + " - cause: " + e.getCause)
        checkOrderResponseMap = Map("code" -> "-1")
      }
    }
    if (checkOrderResponse != null) {
      try {
        checkOrderResponseMap = parseCheckOrderResponse(checkOrderResponse)
      }
      catch {
        case e: EmptyParamException => {
          error(e.getMessage + " - cause: " + e.getCause)
          checkOrderResponseMap = Map("code" -> "-2")
        }
      }
    }
      debug("checkOrderResponse code= " + checkOrderResponseMap("code"))
      checkOrderResponseMap ++ Map("request" -> requestParamsAsMap(checkOrderRequest))
    }

  def performCancelOrder(): Map[String, Object] = {
    var cancelOrderResponse: ContentResponse = null
    var cancelOrderResponseMap: Map[String, Object] = Map.empty
    val cancelOrderRequest=prepareProcessOrderRequest("cancelOrder")
    try {
      cancelOrderResponse = cancelOrderRequest.send()
    }
    catch {
      case e => {
        error(e.getMessage + " - cause: " + e.getCause)
        cancelOrderResponseMap = Map("code" -> "-1")
      }
    }
    if (cancelOrderResponse != null) {
      try {
        cancelOrderResponseMap = parseProcessOrderResponse(cancelOrderResponse)
      }
      catch {
        case e: EmptyParamException => {
          error(e.getMessage + " - cause: " + e.getCause)
          cancelOrderResponseMap = Map("code" -> "-2")
        }
      }
    }
    debug("cancelOrderResponse code= " + cancelOrderResponseMap("code"))
    cancelOrderResponseMap ++ Map("request" -> requestParamsAsMap(cancelOrderRequest))
  }

  def performPaymentAviso: Map[String, Object] = {
    var processOrderResponse: ContentResponse = null
    var timeoutRange = List[Long](5000,10000,15000,20000,30000) //(60000, 300000, 600000, 1200000, 2400000)
    var processOrderResponseMap: Map[String, Object] = Map.empty
    def sendProcessOrderRequest(): Map[String, Object] = {
      val request = prepareProcessOrderRequest("paymentAviso")
      try {
        processOrderResponse = request.send()
      }
      catch {
        case e => error(e)
      }
      if (processOrderResponse == null) {
        error("ProcessOrderResponse = null")
        processOrderResponseMap = Map("code" -> "-1")
        tryResendRequest
      }
      else {
        try {
          processOrderResponseMap = parseProcessOrderResponse(processOrderResponse)
        }
        catch {
          case e => {
            error(e)
            processOrderResponseMap = Map("code" -> "-2")
            tryResendRequest
          }
        }
      }
      processOrderResponseMap ++ Map("request" -> requestParamsAsMap(request))
    }
    def tryResendRequest() {
      debug("sleep time=" + timeoutRange.head)
      if (timeoutRange.nonEmpty) {
        Thread.sleep(timeoutRange.head)
        if (timeoutRange.tail.nonEmpty) {
          timeoutRange = timeoutRange.tail
          sendProcessOrderRequest()
        }
      }
    }
    sendProcessOrderRequest()
  }

  def checkOrderResultProcessing(checkOrderResult: Map[String, Object]): (String,String) = {
    debug("checkOrderRequest params:" + checkOrderResult("request"))
    mdbm.getDatabase()("checkOrderResult").insert(checkOrderResult)
    val code = checkOrderResult.getOrElse("code","").asInstanceOf[String]
    code match {
      case "0" => ("0","Проверка заказа успешна. Магазин дал согласие и готов принять перевод.")
      case "-1" => ("-1","Невозможно совершить платеж, сервер магазина недоступен")
      case "-2" => ("-2","Невозможно разобрать ответ магазина")
      case "1" => ("1","Несовпадение значения параметра md5 с результатом расчета хэш-функции")
      case "100" =>("100","Отказ в приеме перевода с заданными параметрами")
      case "200"=> ("200","Магазин не в состоянии разобрать запрос")
      case s: String => (s,"Неизвестная ошибка")
    }
  }

  def cancelOrderResultProcessing(processOrderResult: Map[String, Object]): (String,String) = {
    debug("processOrderRequest params:" + processOrderResult("request"))
    mdbm.getDatabase()("processOrderResult").insert(processOrderResult)
    val code = processOrderResult.getOrElse("code","").asInstanceOf[String]
    code match {
      case "0" => ("0","Магазин успешно принял уведомление об отмене платежа")
      case "-1" => ("-1","Невозможно передать уведомление, сервер магазина недоступен")
      case "-2" => ("-2","Невозможно разобрать ответ магазина")
      case "1" => ("1","Несовпадение значения параметра md5 с результатом расчета хэш-функции")
      case "200"=> ("200","Магазин не в состоянии разобрать запрос")
      case s : String => (s,"Неизвестная ошибка")
    }
  }

  def paymentAvisoResultProcessing(processOrderResult: Map[String, Object]): (String,String) = {
    mdbm.getDatabase()("processOrderResult").insert(processOrderResult)
    debug("processOrderRequest params:" + processOrderResult("request"))
    val code = processOrderResult.getOrElse("code","").asInstanceOf[String]
    code match {
      case "0" => ("0","Магазин успешно принял платеж")
      case "-1" => ("-1","Невозможно совершить платеж, сервер магазина недоступен")
      case "-2" => ("-2","Невозможно разобрать ответ магазина")
      case "1" =>  ("1","Несовпадение значения параметра md5 с результатом расчета хэш-функции")
      case "200"=> ("200","Магазин не в состоянии разобрать запрос")
      case s : String => (s,"Неизвестная ошибка")
    }
  }

  def run() {
    val checkOrderResult = checkOrderResultProcessing(performCheckOrder)
    if (checkOrderResult._1 == "0") {
      debug("checkOrder: " + checkOrderResult._2)
      Thread.sleep(10000) // Задержка для тестирования ситуации с отключением сервера во время платежного процесса
      Array("paymentAviso", "cancelOrder").apply(Random.nextInt(2))
        match {
          case "paymentAviso" => {
            val paymentAvisoResult = paymentAvisoResultProcessing(performPaymentAviso)
            if (paymentAvisoResult ._1 == "0") debug("paymentAviso: " +  paymentAvisoResult ._2)
            else error("paymentAviso: " +  paymentAvisoResult ._2)
          }
          case "cancelOrder" => {
            val cancelOrderResult = cancelOrderResultProcessing(performCancelOrder)
            if (cancelOrderResult._1 == "0") debug("cancelOrder: " + cancelOrderResult._2)
            else error("cancelOrder: " + cancelOrderResult._2)
          }
        }
    }
    else
      error("checkOrder: " + checkOrderResult._2)
  }
}
