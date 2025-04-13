package ru.sosgps.wayrecall.billing.support

import java.util.Date

import org.bson.types.ObjectId
import org.scalatest.{BeforeAndAfterAll, FunSpec}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.test.context.{ContextConfiguration, TestContextManager}
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.utils.ISODateTime

/**
  * Created by IVAN on 28.02.2017.
  */
@ContextConfiguration(classes = Array(classOf[SupportRequestDAOTestConfig]))
class SupportRequestDAOTest extends FunSpec with BeforeAndAfterAll {

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var srDAO: SupportRequestDAO = null

  new TestContextManager(this.getClass).prepareTestInstance(this)

  override def beforeAll(): Unit = {
    mdbm.getDatabase().dropDatabase()
  }

  override def afterAll(): Unit = {
    mdbm.getDatabase().dropDatabase()
  }

  describe("SupportRequestDAO") {
    val oid = new ObjectId()
    val uid = new ObjectId()
    val supportRequest = SupportRequest(oid, "test", "test", ISODateTime(new Date()), uid, "test", "new", dialog = List.empty, userRead = false, supportRead = false)
    it("insert method") {
      srDAO.insert(supportRequest)
      val sr = srDAO.loadById(oid.toString).get
      assert(sr == supportRequest)
    }
    it("update method") {
      val newUid = new ObjectId()
      val updatedSupportRequest = supportRequest.copy(userId = newUid)
      srDAO.update(updatedSupportRequest)
      val usr = srDAO.loadById(oid.toString).get
      assert(usr == updatedSupportRequest)
      assert(usr.userId == newUid)
    }
    it("remove method") {
      srDAO.remove(oid.toString)
      assert(srDAO.loadById(oid.toString).isEmpty)
    }
  }
}

class SupportRequestDAOTestConfig {

  @Bean
  def mongodbManager: MongoDBManager = {
    val manager = new MongoDBManager()
    manager.databaseName = this.getClass.getSimpleName.replace(".", "-") + "-test"
    manager
  }

  @Bean
  def supportRequestDAO: SupportRequestDAO = new SupportRequestDAO(mongodbManager)
}