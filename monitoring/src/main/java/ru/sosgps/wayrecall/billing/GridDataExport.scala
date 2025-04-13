package ru.sosgps.wayrecall.billing

import java.util
import java.util.Date
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import com.mongodb.casbah.Imports._
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam}
import ru.sosgps.wayrecall.billing.account.AccountsStoreService
import ru.sosgps.wayrecall.billing.dealer.{DealersBalanceHistoryStoreService, DealersService}
import ru.sosgps.wayrecall.billing.equipment.EquipmentStoreService
import ru.sosgps.wayrecall.billing.finance.BalanceHistoryStoreService
import ru.sosgps.wayrecall.billing.user.UsersService
import ru.sosgps.wayrecall.core.{MongoDBManager, UserRolesChecker}
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.utils.web.EDSJsonResponse

import scala.collection.mutable


/**
 * Created by User on 20.08.2014.
 */

@Controller
@ExtDirectService
class GridDataExport extends grizzled.slf4j.Logging {

  @Autowired
  var roleChecker: UserRolesChecker = null

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var accounts: AccountsStoreService = null

  @Autowired
  var objects: AllObjectsService = null

  @Autowired
  var equipments: EquipmentStoreService = null

  @Autowired
  var balanceHistory: BalanceHistoryStoreService = _

  @Autowired
  var dealerBalanceHistory: DealersBalanceHistoryStoreService = _

  @Autowired
  var users: UsersService = null

  def fieldNamesTranslate(entity:String ) = {
    val translateMap=entity match {
      case "equipments" =>Map[String,String]("_id"->"ID","objectName"->"Объект","accountId"->"ID учетной записи",
        "accountName"->"Учетная запись","eqOwner"->"Собственник","eqRightToUse"->"Основание использования",
        "eqSellDate"->"Дата продажи","eqWork"->"Работа","eqWorkDate"->"Дата работы","eqNote"->"Примечание",
        "eqtype"->"Тип","eqMark"->"Марка","eqModel"->"Модель","eqSerNum"->"Серийный номер",
        "eqIMEI"->"IMEI","eqFirmware"->"Прошивка","eqLogin"->"Логин","eqPass"->"Пароль",
        "simOwner"->"Собственник SIM","simProvider"->"Оператор","simNumber"->"Номер","simICCID"->"ICCID",
        "simNote"->"Примечание SIM","instPlace"->"Место установки")
      case "accounts" => Map[String,String]("_id"->"ID", "name"->"Имя","comment"->"Комментарий","plan"->"Тариф",
        "balance"->"Баланс","cost"->"Абонентская плата", "objectsCount"->"Кол-во объектов",
        "equipmentsCount"->"Кол-во оборудования", "usersCount"->"Кол-во пользователей","status"->"Заблокирован")
      case "objects" =>Map[String,String]("_id"->"ID","account"->"ID учетной записи","accountName"->"Учетная запись",
        "name"->"Имя","customName"->"Пользовательское имя","comment"->"Комментарий","uid"->"UID","type"->"Тип",
        "contract"->"Договор","cost"->"Абонентская плата","marka"->"Марка","model"->"Модель","gosnumber"->"Госномер",
        "VIN"->"VIN"/*,"instplace"*/,"blocked"->"Заблокирован","ignition"->"Зажигание",/*"objnote",*/
        "latestmsg"->"Последнее сообщение","latestmsgprotocol"->"Протокол","sms"->"СМС","speed"->"Скорость",
        "satelliteNum"->"Число спутников","placeName"->"Последнее положение","sleeper"->"Спящий","" +
        "sleepertime"->"Время сообщения от спящего","trackerModel"->"Тип оборудования","disabled"->"Отключен",
        "eqIMEI"->"IMEI", "simNumber"->"Номер"/*,"eqMark","eqModel"*/)
      case "users" => Map[String,String]("_id"->"ID", "name"->"Имя", "comment"->"Комментарий", "password"->"Пароль",
        "email"->"Почта","lastLoginDate"->"Последний вход", "lastAction"->"Последнее действие",
        "mainAccId"->"ID основной учетной записи", "mainAccName"->"Основная учетная запись",
        "hascommandpass"->"Наличие пароля для команд","commandpass"->"Пароль для команд", "enabled"->"Включен",
        "blockcause"->"Причина блокировки","canchangepass"->"Может изменять пароль",
        "showbalance"->"Может видеть баланс","showfeedetails"->"Может видеть детализацию","userType"->"Тип пользователя",
        "creator"->"Создатель пользователя")
      case "balance" => Map("type" -> "Тип", "ammount" -> "Сумма", "timestamp" -> "Время", "newbalance" -> "Баланс", "comment" -> "Комментарий")
      case "dealerBalance" => Map("type" -> "Тип", "ammount" -> "Сумма", "timestamp" -> "Время", "newbalance" -> "Баланс", "comment" -> "Комментарий")
    }
    getGridFieldNames(entity).map(name=>translateMap(name)).toIndexedSeq
  }

  def getGridFieldNames(entity:String): mutable.LinkedHashSet[String] = {
    val roles=roleChecker.getUserAuthorities()
    val adminParams: mutable.LinkedHashSet[String] = entity match {
      case "equipments" => mutable.LinkedHashSet("_id",/*"objectid",*/"objectName","accountId","accountName",
        //Вкладка Основные сведения
        "eqOwner","eqRightToUse","eqSellDate","eqWork","eqWorkDate","eqNote",
        //Вкладка Характеристики устройства
        "eqtype","eqMark","eqModel","eqSerNum",
        "eqIMEI","eqFirmware","eqLogin","eqPass",
        //Вкладка SIM-карта
        "simOwner","simProvider","simNumber","simICCID","simNote",
        //Место установки
        "instPlace")
      case "accounts" => mutable.LinkedHashSet("_id", "name","comment","plan", "balance",
        "cost", "objectsCount","equipmentsCount", "usersCount","status")
      case "objects" =>mutable.LinkedHashSet("_id","uid","account","accountName","name","customName","comment",
        "type","cost","marka","model","gosnumber","VIN"/*,"instplace"*/,
        "blocked","ignition",/*"objnote",*/"latestmsg","latestmsgprotocol","sms","speed","satelliteNum",
        "placeName","sleeper","sleepertime","trackerModel","disabled","eqIMEI", "simNumber"/*,"eqMark","eqModel"*/)
      case "users" => mutable.LinkedHashSet("_id", "name", "comment", /*"password",*/ "email",
        "lastLoginDate", "lastAction", "mainAccId", "mainAccName","hascommandpass",
        "commandpass", "enabled", "blockcause","canchangepass","showbalance","showfeedetails","userType","creator")
      case "balance" => mutable.LinkedHashSet("type" , "ammount" , "timestamp", "newbalance", "comment")
      case "dealerBalance" => mutable.LinkedHashSet("type" , "ammount" , "timestamp", "newbalance", "comment")
      case _ => mutable.LinkedHashSet.empty
    }
    val superUserParams: mutable.LinkedHashSet[String] =
      entity match {
        case "equipments" => mutable.LinkedHashSet("_id", /*"objectid",*/ "objectName", "accountId", "accountName",
          "eqOwner", "eqRightToUse", "eqSellDate", "eqWork", "eqWorkDate", "eqNote",
          "eqtype", "eqMark", "eqModel", "eqSerNum","eqIMEI", "eqFirmware", "instPlace")
        case "accounts" => mutable.LinkedHashSet("_id", "name","comment","plan", "balance",
          "cost", "objectsCount","equipmentsCount", "usersCount","status")
        case "objects" =>mutable.LinkedHashSet("_id","uid","account","accountName","name","customName","comment",
          "type","cost","marka","model","gosnumber","VIN",/*"instplace",*/
          "blocked","ignition",/*"objnote",*/"latestmsg","latestmsgprotocol","sms","speed","satelliteNum",
          "placeName","sleeper","sleepertime","trackerModel","disabled"/*,"eqMark","eqModel"*/)
        case "users" => mutable.LinkedHashSet("_id", "name", "comment", /*"password",*/ "email",
          "lastLoginDate", "lastAction", "mainAccId", "mainAccName", "hascommandpass",
          "commandpass", "enabled", "blockcause", "canchangepass", "showbalance", "showfeedetails", "userType", "creator")
        case "balance" => mutable.LinkedHashSet("type" , "ammount" , "timestamp", "newbalance", "comment")
        case "dealerBalance" => mutable.LinkedHashSet("type" , "ammount" , "timestamp", "newbalance", "comment")
        case _ => mutable.LinkedHashSet.empty
      }

    if(roles.contains("admin")) {
      adminParams
    }
    else if(roles.contains("superuser")){
      superUserParams
    }
    else mutable.LinkedHashSet.empty
  }

  private def makeBalanceRequest(request: HttpServletRequest) = {
    val r = new ExtDirectStoreReadRequest()
    val params = new util.HashMap[String, AnyRef]()
    for(paramName <- Seq("dateFrom", "dateTo", "accountId", "itemType")) {
      params.put(paramName, request.getParameter(paramName))
    }
    if(request.getParameterValues("typeFilter") != null)
      params.put("typeFilter", java.util.Arrays.asList[String](request.getParameterValues("typeFilter") : _*))
    r.setParams(params)
    r
  }

  def currencyFormat(amount: Long) =
  {
    val strAmount = amount.toString
    if(strAmount.length >= 2)
      strAmount.substring(0, strAmount.length - 2) + "." + strAmount.substring(strAmount.length - 2) + " р."
    else {
      "0." + strAmount +" р."
    }
  }
  def read (entity:String, request: HttpServletRequest) = {
    val jodaISOformat = DateTimeFormat.forPattern("y-MM-dd HH:mm:ss")
    def format(d: Date) = jodaISOformat.print(new DateTime(d))
    val items = entity match {
      case "equipments" => equipments.getAllEquipments(new ExtDirectStoreReadRequest()) //.asInstanceOf[List[DBObject]]
      case "accounts" => accounts.getAllAccounts(new ExtDirectStoreReadRequest()) //.asInstanceOf[List[DBObject]]
      case "objects" => objects.getAllObjects(new ExtDirectStoreReadRequest()) //.asInstanceOf[List[DBObject]]
      case "users" => users.getAllUsers(new ExtDirectStoreReadRequest()) //.asInstanceOf[List[DBObject]]
      case "balance" | "dealerBalance" =>
        val lst = balanceHistory.loadRawData(makeBalanceRequest(request)).map(d => d: MongoDBObject)
          .toList.sortWith((d1,d2) => Ordering[Date].gt(d1.as[Date]("timestamp"),d2.as[Date]("timestamp")))
        for( d <- lst) {
          d.put("timestamp", format(d.as[Date]("timestamp")))
          d.put("ammount", currencyFormat(d.as[Long]("ammount")))
          d.put("newbalance", currencyFormat(d.as[Long]("newbalance")))
        }
        lst
    }

    items.map(_.filter(item => getGridFieldNames(entity).contains(item._1))).toStream
  }



  @RequestMapping(Array("/gridDataExport"))
  def process(
               @RequestParam("entity") entity: String,
               //@RequestParam("from") from: Date,
               //@RequestParam("to") to: Date,
               request: HttpServletRequest,
               response: HttpServletResponse) {
    response.setContentType("application/octet-stream")
    response.setHeader("Content-Disposition", "attachment; filename=\""+entity+"_report.xlsx\"");

    val workbook = new XSSFWorkbook();
    val sheet1 = workbook.createSheet(entity)

    val fieldNames = fieldNamesTranslate(entity)
    val nameMap=getGridFieldNames(entity).zipWithIndex.toMap

    val headerRow=sheet1.createRow(0)
    for(i <- 1 to fieldNames.length){
      headerRow.createCell(i-1).setCellValue(fieldNames(i-1))
    }

    for((m,i) <- read(entity, request).zipWithIndex){
      val row = sheet1.createRow(i+1)
      nameMap.foreach(item=>
        {
          val value=m.get(item._1).getOrElse(" ").toString
          row.createCell(item._2).setCellValue(value)
        }
      )
    }

    for (i <- 0 to fieldNames.length) {
      sheet1.autoSizeColumn(i)
    }

    val out = response.getOutputStream
    workbook.write(out)
    out.close()

  }

}
