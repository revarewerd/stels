package ru.sosgps.wayrecall.monitoring.notifications

import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.monitoring.notifications.rules.MaintenanceNotificationRule

import scala.collection.immutable.List


class NotificationRuleLoaderTest {


  @Test
  def test(): Unit = {

    val rule = MongoDBObject(
      "_id" -> new ObjectId(),
      "name" -> "Тест прохождения дистанции",
      "email" -> "",
      "params" -> Map(
        "ntfDistMax" -> "0.2",
        "ntfMotoMax" -> 0.3
      ),
      "showmessage" -> true,
      "objects" -> List(
        "o1603556978227868060"
      ),
      "type" -> "ntfMntnc",
      "user" -> "1234",
      "messagemask" -> "Требуется техобслуживание",
      "phone" -> "+791604108305"
    )

    val r = new NotificationRuleLoader(List(classOf[MaintenanceNotificationRule]))
      .load(rule, null).asInstanceOf[MaintenanceNotificationRule]

    Assert.assertEquals(0.2, r.ntfDistMax, 0.0)
    Assert.assertEquals(0.3, r.ntfMotoMax, 0.0)
    Assert.assertEquals(0L, r.ntfhoursMax, 0.0)


  }


}
