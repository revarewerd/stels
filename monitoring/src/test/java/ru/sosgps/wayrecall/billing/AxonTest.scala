package ru.sosgps.wayrecall.billing

import org.springframework.context.support.ClassPathXmlApplicationContext
import org.axonframework.commandhandling.gateway.CommandGateway
import ru.sosgps.wayrecall.billing.objectcqrs.{ObjectDataSetCommand, ObjectCreateCommand}
import com.mongodb.MongoClient
import scala.util.Random
import org.bson.types.ObjectId

/**
 * Created by nickl on 04.01.14.
 */
object AxonTest {

  val r = new Random()

  var uid: String = null

  val eqIds = Seq.fill(3)(new ObjectId())

  def main(args: Array[String]) {

    initAxon()

    reuseAxon()

  }


  private[this] def initAxon() {
    val context = createContext

    val mongoClient = context.getBean(classOf[ru.sosgps.wayrecall.core.MongoDBManager])

    val dB = mongoClient.getDatabase()
    println("droppingDatabase "+dB.name)
    dB.dropDatabase()

    val commandGateway = context.getBean("commandGateway", classOf[CommandGateway])

    //uid = commandGateway.sendAndWait[String](new ObjectCreateCommand())

    uid = "testuid"

    require(uid != null, "uid cant be null")
    println("object with uid=" + uid)

    commandGateway.send(new ObjectDataSetCommand(uid, Map("foo" -> "bar"), Seq.empty))


    for (x <- 1 to 7 ) {
      commandGateway.send(
        new ObjectDataSetCommand(
          uid,
          Map(randomKV),
          eqIds.map(oid => Map ("_id" -> oid, randomKV)))
      )
    }


    context.close();
  }


  private[this] def randomKV: (String, String) = {
    r.alphanumeric.take(5).mkString -> r.alphanumeric.take(6).mkString
  }

  private[this] def reuseAxon() {
    val context = createContext

    val commandGateway = context.getBean("commandGateway", classOf[CommandGateway])

    commandGateway.send(
      new ObjectDataSetCommand(
        uid,
        Map("foo2" -> "bar2"),
        eqIds.map(oid => Map ("_id" -> oid, randomKV)))
      )

    context.close();
  }

  private[this] def createContext: ClassPathXmlApplicationContext = {
    new ClassPathXmlApplicationContext("axon-test-config.xml")
  }
}
