package ru.sosgps.wayrecall.monitoring

import ch.ralscha.extdirectspring.annotation.ExtDirectMethod
import com.mongodb.casbah.commons.MongoDBObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import ru.sosgps.wayrecall.core.{MongoDBManager, UserRolesChecker}
import ru.sosgps.wayrecall.payment.PaymentParams
import ru.sosgps.wayrecall.utils.ExtDirectService
import com.mongodb.casbah.Imports._

import scala.collection.JavaConversions.asScalaIterator

/**
 * Created by IVAN on 15.03.2016.
 */

@Controller
@ExtDirectService
class PaymentClientService extends grizzled.slf4j.Logging {

  @Autowired
  var roleChecker: UserRolesChecker = null

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var paymentParams: PaymentParams = null


  private def getMainAccountId(): ObjectId = {
    val username = roleChecker.getUserName()
    val userOption = mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> username), MongoDBObject("mainAccId" -> 1))
    userOption match {
      case Some(user) => {
        val mainAccIdOption = user.getAs[ObjectId]("mainAccId")
        mainAccIdOption match {
          case Some(mainAccId) => {
            mainAccId
          }
          case None => throw new NoSuchElementException("payment.mainAccUndefined")
        }
      }
      case None => throw new NoSuchElementException("payment.noSuchUser")
    }
  }


  private def hasPaymentSystem(): Boolean = {
    paymentParams.shopId != "-1"
  }


   private def checkPaymentWay(): Boolean = {
    try {
      val mainAccId=getMainAccountId()
      val accountOption=mdbm.getDatabase()("accounts").findOne(MongoDBObject("_id" -> mainAccId))
      accountOption match  {
        case Some(account) => {
          val paymentWay=account.getOrElse("paymentWay","")
          paymentWay=="yandexPayment"
        }
        case None => false
      }
    }
    catch {
      case e => false
    }
  }

  @ExtDirectMethod
  def canMakePayment() :Boolean = {
    hasPaymentSystem && checkPaymentWay
  }

  @ExtDirectMethod
  def getPaymentParams() : Map[String,String] = {
    if (hasPaymentSystem()) {
      try {
        val mainAccId=getMainAccountId()
        Map("type" -> "success", "shopId" -> paymentParams.shopId, "scId" -> paymentParams.scId,
          "customerNumber" -> mainAccId.toString, "paymentServerUrl" -> paymentParams.paymentServerUrl)
      }
      catch {
        case e => Map("type" -> "error", "result" -> e.getMessage)
      }

    }
    else Map("type" -> "error", "result" -> "payment.notConnected")
  }
}
