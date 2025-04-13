package ru.sosgps.wayrecall.tools

import com.beust.jcommander.{Parameter, Parameters};
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.{MutablePropertySources, PropertiesPropertySource, StandardEnvironment}
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.{GrantedAuthority, Authentication}
import ru.sosgps.wayrecall.billing.tariff.commands.TariffPlanCreateCommand
import ru.sosgps.wayrecall.billing.user.commands.UserCreateCommand
import ru.sosgps.wayrecall.billing.user.permission.commands.PermissionCreateCommand
import ru.sosgps.wayrecall.core.{UserRolesChecker, SecureGateway, MongoDBManager}
import ru.sosgps.wayrecall.jcommanderutils.CliCommand
import ru.sosgps.wayrecall.security.PermissionsManager
import scala.util.control.Exception
import java.io.File
import java.util.{Properties, HashMap}
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.eventstore.EventStreamNotFoundException
import org.axonframework.eventstore.mongo.MongoEventStore
import org.springframework.context.ApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import ru.sosgps.wayrecall.billing.account.AccountAggregate

import ru.sosgps.wayrecall.billing.equipment.types.commands.EquipmentTypesCreateCommand
import ru.sosgps.wayrecall.billing.equipment.EquipmentCreateCommand
import ru.sosgps.wayrecall.billing.objectcqrs.ObjectAggregate
import ru.sosgps.wayrecall.billing.account.commands.AccountCreateCommand
import ru.sosgps.wayrecall.billing.objectcqrs.ObjectCreateCommand
import org.axonframework.serializer.bson.DBObjectXStreamSerializer
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.JavaConverters.mapAsJavaMapConverter


@Parameters(commandDescription = "Create aggregates for selected type (usually collection name) of items")
object AggregateCreator extends CliCommand {

  @Parameter(names = Array("-t", "-type"), required = false)
  var itemTypes: java.util.List[String] = null

  @Parameter(names = Array("-d", "-db"), required = false, description = "database name, default loaded from config")
  var dbname: String = null

  val commandName = "createaggregate"
  var allAuthorities = Set(
    "admin",
    "AccountView", "AccountCreate", "AccountDataSet", "AccountDelete",
    "TariffView", "TariffPlanCreate", "TariffPlanDataSet", "TariffPlanDelete",
    "EquipmentView", "EquipmentCreate", "EquipmentDataSet", "EquipmentDelete",
    "ObjectView", "ObjectCreate", "ObjectDataSet", "ObjectDelete", "ObjectRemove", "ObjectRestore", "EquipmentRemove", "EquipmentRestore",
    "EquipmentTypesView", "EquipmentTypesCreate", "EquipmentTypesDataSet", "EquipmentTypesDelete",
    "UserView", "UserCreate", "UserDataSet", "UserDelete",
    "ChangeRoles",
    "ChangePermissions",
    "ChangeBalance"
  )

  var mdbm: MongoDBManager = null
  var context: ClassPathXmlApplicationContext = null

  def process() {
    context = new ClassPathXmlApplicationContext();
    context.setConfigLocation("axon-config.xml");

    if (dbname != null) {
      context.setEnvironment(new StandardEnvironment {
        protected override def customizePropertySources(propertySources: MutablePropertySources) {
          super.customizePropertySources(propertySources)
          val generatedProps: Properties = new Properties
          generatedProps.setProperty("instance.defaultmongodb.databaseName", dbname)
          propertySources.addFirst(new PropertiesPropertySource("generated", generatedProps))
        }
      })
    }

    context.refresh();
    context.registerShutdownHook();
    mdbm = context.getBean(classOf[MongoDBManager]);
    if (itemTypes == null)
      itemTypes = mdbm.getDatabase().collectionNames.toBuffer
        .filter(cn => getAggregateParams.isDefinedAt(DBObject.empty, cn))


    for (itemType <- itemTypes) {
      println("Try to create aggregates for " + itemType)

      mdbm.getDatabase().apply(itemType).find().foreach(item => try {
        createAggregate(getAggregateParams(item, itemType) /*,context*/)
      } catch {
        case e: Exception => e.printStackTrace()
      })
    }

    println("completed!")

  }

  private[this] val getAggregateParams: PartialFunction[(DBObject, String), AggregateParams] = {
    // (item: DBObject, itemType: String)

    case (item, "accounts") => {
      val id = item.as[ObjectId]("_id")
      val data = item.mapValues(_.asInstanceOf[AnyRef]).toMap
      new AggregateParams("AccountAggregate", id, new AccountCreateCommand(id, data.asJava))
    }
    case (item, "users") => {
      val id = item.as[ObjectId]("_id")
      val data = item.mapValues(_.asInstanceOf[AnyRef]).toMap
      new AggregateParams("UserAggregate", id, new UserCreateCommand(id, data.asJava))
    }
    case (item, "usersPermissions") => {
      val id = item.as[ObjectId]("_id")
      val data = item.mapValues(_.asInstanceOf[AnyRef]).toMap
      new AggregateParams("PermissionAggregate", id, new PermissionCreateCommand(id, data.asJava))
    }
    case (item, "objects") => {
      val uid = item.as[String]("uid")
      val objdata = item.mapValues(_.asInstanceOf[java.io.Serializable]).toMap
      val eqs = mdbm.getDatabase().apply("equipments").find(MongoDBObject("uid" -> item.as[String]("uid")))
      val eqdata = eqs.map(eq => {
        (eq: MongoDBObject).mapValues(_.asInstanceOf[java.io.Serializable]).toMap
      }).toSeq

      new AggregateParams("ObjectAggregate", uid, new ObjectCreateCommand(uid, objdata, eqdata))
    }
    case (item, "equipments") => {
      val id = item.as[ObjectId]("_id")
      val data = item.mapValues(_.asInstanceOf[java.io.Serializable]).toMap
      new AggregateParams("EquipmentAggregate", id, new EquipmentCreateCommand(id, data))
    }
    case (item, "equipmentTypes") => {
      val id = item.as[ObjectId]("_id")
      val data = item.mapValues(_.asInstanceOf[AnyRef]).toMap
      new AggregateParams("EquipmentTypesAggregate", id, new EquipmentTypesCreateCommand(id, data.asJava))
    }
    case (item, "tariffs") => {
      val id = item.as[ObjectId]("_id")
      val data = item.mapValues(_.asInstanceOf[AnyRef]).toMap
      new AggregateParams("TariffPlanAggregate", id, new TariffPlanCreateCommand(id, data.asJava))
    }
    //case _ => throw new IllegalArgumentException("No such aggregate type for " + itemType)
  }

  private[this] def createAggregate(aggregateParams: AggregateParams) {
    require(aggregateParams.id != null, "item identifier cant be null")
    val eventStore: MongoEventStore = context.getBean(classOf[MongoEventStore])
    val commandGateway: SecureGateway = context.getBean(classOf[SecureGateway])
    val exists = Exception.catching(classOf[EventStreamNotFoundException]).opt(
      eventStore.readEvents(aggregateParams.name, aggregateParams.id).hasNext).getOrElse(false)
    if (!exists) {
      System.out.println("Aggregate for item " + aggregateParams.id + " not exist")
      commandGateway.sendAndWait(aggregateParams.command,allAuthorities.toArray ,"System");
      System.out.println("Create " + aggregateParams.name + " with id= " + aggregateParams.id + " with data=" + aggregateParams.command)
    } else {
      System.out.println("Aggregate for item " + aggregateParams.id + " already exist")
    }
  }
}

class AggregateParams(val name: String, val id: AnyRef, val command: AnyRef)

class RoleChecker extends PermissionsManager with UserRolesChecker with grizzled.slf4j.Logging{

  @Autowired
  var mdbm: MongoDBManager = null

  def getPermForCurrentUser() = {
    var allObjects : Iterator[DBObject] = Iterator.empty
    val userName = getUserName()
    if(hasRequiredAuthorities(Seq(new SimpleGrantedAuthority("admin"),new SimpleGrantedAuthority("UserView")))) {
      allObjects = mdbm.getDatabase().apply("users").find(MongoDBObject())
    }
    else if(hasRequiredAuthorities(Seq(new SimpleGrantedAuthority("superuser"),new SimpleGrantedAuthority("UserView")))) {
      val creatorId=getUserIdByName(userName)
      val creator=if(creatorId==null) userName else creatorId
      allObjects = mdbm.getDatabase().apply("users").find($or("name"->userName,"creator"->creator))
    }
    allObjects
  }

  def hasRequiredAuthorities(requiredAuth: Seq[GrantedAuthority])={
    val userAuth=SecurityContextHolder.getContext().getAuthentication().getAuthorities
    var hasAuth=true
    requiredAuth.foreach(auth=>{
      if(!userAuth.contains(auth)) {hasAuth=false; debug("Hasn't got required authority - "+auth.toString)}
    })
    hasAuth
  }
}