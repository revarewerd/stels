package ru.sosgps.wayrecall.core

/**
 * Created by IVAN on 20.05.2014.
 */
object WayrecallAggregateTranslate {
  val AccountTranslate = Map("_id"->"id", "name"->"учетная запись", "comment"->"комментарий",
    "fullClientName"->"полное наименование","accountType"->"тип учетной записи",  "plan"->"тарифный план",
    "status"->"включен","blockcause"->"причина блокировки","cffam"->"фамилия", "cfname"->"имя",
    "cffathername"->"отчество", "cfmobphone1"->"мобильный телефон 1","cfmobphone2"->"мобильный телефон 2",
    "cfworkphone1"->"рабочий телефон", "cfemail"->"e-mail", "cfnote"->"примечание","contractCount"->"число договоров",
    "contracts"->"контракты",
    "conNumber"->"№ договора","conDate"->"Дата заключения","conPerson"->"Кем заключен","conCodeWord"->"Кодовое слово",
    "conNote"->"Примечание",
    //Контракт физ-лиц
    "borndate"->"Дата рождения","bornplace"->"Место рождения",
    "passportn"->"серия номер паспорта","passpissuedby"->"кем выдан паспорт","passpissueddate"->"когда выдан паспорт",
    "propcountry"->"прописка: страна","propindex"->"прописка: индекс","proptersubj"->"прописка: субъект",
    "propzone"->"прописка: район","propcity"->"прописка: город","propstreet"->"прописка: улица",
    "prophouse"->"прописка: дом","propflat"->"прописка: квартира",
    "prozhcountry"->"проживание: страна","prozhindex"->"проживание: индекс","prozhtersubj"->"проживание: субъект",
    "prozhzone"->"проживание: район","prozhcity"->"проживание: город","prozhstreet"->"проживание: улица",
    "prozhhouse"->"проживание: дом","prozhflat"->"проживание: квартира",
    //Котракт юр-лиц
    "ucountry"->"юр.адрес: страна","uindex"->"юр.адрес: индекс","utersubj"->"юр.адрес: субъект",
    "uzone"->"юр.адрес: район","ucity"->"юр.адрес: город","ustreet"->"юр.адрес: улица",
    "uhouse"->"юр.адрес: дом","uoffice"->"юр.адрес: офис",
    "fcountry"->"физ.адрес: страна","findex"->"физ.адрес: индекс","ftersubj"->"физ.адрес: субъект",
    "fzone"->"физ.адрес: район","fcity"->"физ.адрес: город","fstreet"->"физ.адрес: улица",
    "fhouse"->"физ.адрес: дом","foffice"->"физ.адрес: офис",
    "bankname"->"банк","currentacc"->"расчетный счет","bik"->"БИК","correspondacc"->"корреспондентский счет",
    "inn"->"ИНН","kpp"->"КПП","okpo"->"ОКПО","okved"->"ОКВЭД","banknote"->"примечание",
    "gdfam"->"ген.дир: фамилия","gdname"->"ген.дир: имя","gdfathername"->"ген.дир: отчество",
    "gdphone1"->"ген.дир: телефон1","gdphone2"->"ген.дир: телефон2","gdstreet"->"ген.дир: улица",
    "gdhouse"->"ген.дир: дом","gdoffice"->"ген.дир: офис",
    "gbfam"->"гл.бух: фамилия","gbname"->"гл.бух: имя","gbfathername"->"гл.бух: отчество",
    "gbphone"->"гл.бух: телефон","gbstreet"->"гл.бух: улица",
    "gbhouse"->"гл.бух: дом","gboffice"->"гл.бух: офис", "balance" -> "баланс"
  )

  val ObjectTranslate = Map(
    "name"->"наименование","customName"->"пользовательское имя", "comment"->"комментарий", "uid"->"uid", "type"->"тип",
    "subscriptionfee"->"абонентская плата", "marka"->"марка", "account"->"учетная запись","phone"->"телефон","_id"->"id",
    "accountName"->"учетная запись","model"->"модель", "gosnumber"->"госномер", "VIN"->"VIN", "objnote"->"примечание",
    "equipmentType"->"трекер","fuelPumpLock"->"блок. бензонасоса","ignitionLock"->"блок. зажигания",
    "accountId"->"ID учетной записи", "prevAccount" -> "Учетная запись"
  )

  val UserTranslate = Map(
    "_id"->"id", "name"->"имя", "comment"->"комментарий", "password"->"пароль", "email"->"e-mail",
    "lastLoginDate"->"последний вход", "lastAction"->"последнее действие", "mainAccId"->"id основной учетной записи",
    "mainAccName"->"основная учетная запись", "hascommandpass"->"наличие пароля комманд", "commandpass"->"пароль для комманд",
    "enabled"->"включен", "blockcause"->"причина блокировки","canchangepass"->"разрешить изменение пароля",
    "showbalance"->"показывать баланс","showfeedetails"->"показывать детализацию"
  )

  val PermissionTranslate = Map(
    "_id"->"id", "item_id"->"id объекта", "name"->"имя объекта", "view"->"просмотр", "sleepersView"->"просмотр спящих",
    "control"->"управление", "customName"->"пользовательское имя", "uid"->"uid", "recordType"->"тип объекта",
    "userId"->"id пользователя"
  )

  val EquipmentTranslate = Map(
    "_id"->"id","eqOwner"->"cобственник","eqRightToUse"->"основание использования","eqSellDate"->"дата продажи",
    "eqWork"->"работа","eqWorkDate"->"дата работы","eqNote"->"примечание","eqtype"->"тип","eqMark"->"марка",
    "eqModel"->"модель","eqSerNum"->"серийный номер","eqIMEI"->"IMEI","eqFirmware"->"прошивка",
    "eqConfig"->"конфигурация","eqLogin"->"логин","eqPass"->"пароль","simOwner"->"владелец sim","simProvider"->"провайдер",
    "simSetDate"->"дата установки sim","simNumber"->"абонентский номер","simICCID"->"ICCID","simNote"->"sim примечание",
    "instPlace"->"место установки","accountId"->"ID учетной записи", "prevUid" -> "Имя объекта"
  )

   val EquipmentTypeTranslate = Map(
     "_id"->"id","type"->"тип устройства","mark"->"марка", "model"->"модель", "server"->"сервер","port"->"порт",
     "paramId"->"id параметра","paramName"->"имя параметра", "unit" -> "единица измерения"
   )

   val TariffPlanTranslate = Map(
     "_id"->"id", "name"->"имя","comment"->"комментарий"
   )

   val TicketTranslate = Map(
     "_id"->"id", "name"->"имя","status"->"статус","assignee"->"назначен","creator"->"создатель","openDate"->"дата открытия","closeDate"->"дата закрытия","data"->"данные"
    )


}
