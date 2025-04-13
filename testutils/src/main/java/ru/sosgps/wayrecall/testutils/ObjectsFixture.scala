package ru.sosgps.wayrecall.testutils

import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.core.MongoDBManager

/**
 * Created by nickl on 18.12.14.
 */
trait ObjectsFixture {

  def mdbm: MongoDBManager

  protected def ensureExists(uid: String) {
    require(uid != null)
    val q = MongoDBObject("uid" -> uid)
    mdbm.getDatabase()("objects").update(q, $set(q.toSeq:_*), upsert = true)
  }

  protected def ensureExists(uid: String, imei: String) {
    ensureExists(uid)
    val q = MongoDBObject("uid" -> uid, "eqIMEI" -> imei, "eqtype" -> "Основной абонентский терминал")
    mdbm.getDatabase()("equipments").update(q, $set(q.toSeq:_*), upsert = true)
  }
}
