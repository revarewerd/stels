package ru.sosgps.wayrecall.core

import com.mongodb.casbah.Imports._
import org.axonframework.commandhandling.CommandDispatchInterceptor
import org.axonframework.commandhandling.CommandMessage
import org.axonframework.domain.MetaData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.{Objects, Arrays, Iterator}
import ru.sosgps.wayrecall.security.PermissionsManager
import org.bson.types.ObjectId
import ru.sosgps.wayrecall.utils.errors.NotPermitted

@Component
class SecureInterceptor extends CommandDispatchInterceptor with grizzled.slf4j.Logging {

  @Autowired
  var roleChecker: UserRolesChecker = null

//  @Autowired
//  var or: ObjectsRepositoryReader= null

  val commandAuthoritiesMap = Map(
    "AccountCreateCommand" ->"AccountCreate",
    "AccountDataSetCommand" ->"AccountDataSet",
    "AccountDeleteCommand" -> "AccountDelete",
    "TariffPlanCreateCommand" -> "TariffPlanCreate",
    "TariffPlanDataSetCommand" -> "TariffPlanDataSet",
    "TariffPlanDeleteCommand" -> "TariffPlanDelete",
    "EquipmentCreateCommand" -> "EquipmentCreate",
    "EquipmentDataSetCommand" -> "EquipmentDataSet",
    "EquipmentDeleteCommand" -> "EquipmentDelete",
    "EquipmentObjectChangeCommand" -> "EquipmentDataSet",
    "ObjectCreateCommand" -> "ObjectCreate",
    "ObjectDataSetCommand" -> "ObjectDataSet",
    "ObjectDeleteCommand" -> "ObjectDelete",
    "ObjectRestoreCommand" -> "ObjectRestore",
    "ObjectRemoveCommand" -> "ObjectRemove",
    "EquipmentRemoveCommand" -> "EquipmentRemove",
    "EquipmentRestoreCommand" -> "EquipmentRestore",
    "EquipmentTypesCreateCommand" -> "EquipmentTypesCreate",
    "EquipmentTypesDataSetCommand" -> "EquipmentTypesDataSet",
    "EquipmentTypesDeleteCommand" -> "EquipmentTypesDelete",
    "UserCreateCommand" -> "UserCreate",
    "UserDataSetCommand" -> "UserDataSet",
    "UserDeleteCommand" -> "UserDelete",
    "PermissionCreateCommand" -> "ChangePermissions",
    "PermissionDataSetCommand" -> "ChangePermissions",
    "PermissionDeleteCommand" -> "ChangePermissions",
    "DealerTarifficationChangeCommand" -> "DealerTariffer",  
    "TicketCreateCommand" -> "Manager",
    "TicketDataSetCommand" -> "Manager",
    "TicketDeleteCommand" -> "Manager"
  )

  def handle(commandMessage: CommandMessage[_]): CommandMessage[_] = {
    val commandName = commandMessage.getPayloadType.getSimpleName
    val entity=commandMessage.getPayload.asInstanceOf[CommandEntityInfo].getEntity()
    val entityId=commandMessage.getPayload.asInstanceOf[CommandEntityInfo].getEntityId()
    val roles: Array[String] = Objects.requireNonNull(commandMessage.getMetaData.get("userRoles").asInstanceOf[Array[String]], "userRoles must be set")
    val userName = commandMessage.getMetaData.get("userName").asInstanceOf[String]
    debug("Entity=" +entity+", id="+entityId)
    debug("Command name=" + commandName)
    if(roles.contains("admin")|| roles.contains("servicer")){
      checkHasAuthority(commandName,roles,commandAuthoritiesMap.getOrElse(commandName, "admin"))
    }
    else if(roles.contains("superuser")){
      checkHasAuthority(commandName,roles,commandAuthoritiesMap.getOrElse(commandName, "admin"))
      checkUserTypePermissions(commandName,roles,userName,entity, entityId)
    }
    else if(roles.contains("ROLE_USER")){
      checkUserTypePermissions(commandName,roles,userName,entity, entityId)
    }
    return commandMessage
  }

  def checkUserTypePermissions(commandName:String,roles:Array[String], userName: String,entity: String, entityId: Any){
    if(roles.contains("superuser")){
      entity match {
        case "User" if(!commandName.contains("Create")) => {
          if(!roleChecker.hasPermissions(userName,entity.toLowerCase(),entityId)) throw new NotPermitted("Нет прав на " +entity +"  id="+entityId )
          debug("Allow "+commandName+" command")
        }
        case "Permission" | "User" => debug("Allow "+commandName+" command")
        case _ =>  throw new NotPermitted("Нет прав на " +entity +"  id="+entityId )
      }
    }
    else if(roles.contains("ROLE_USER")){
      entity match {
        case "Object" if(commandName.contains("DataSet")) => {
          if(!roleChecker.hasPermissions(userName,entity.toLowerCase(),entityId)) throw new NotPermitted("Недостаточно прав на " +entity +"  id="+entityId )
          debug("Allow "+commandName+" command")
        }
        case "User" if(commandName.contains("DataSet")) => {
          if(!roleChecker.hasPermissions(userName,entity.toLowerCase(),entityId)) throw new NotPermitted("Нет прав на " +entity +"  id="+entityId )
          debug("Allow "+commandName+" command")
        }
        case _ =>  throw new NotPermitted("Нет прав на " +entity +"  id="+entityId )
      }
    }
    else
    throw new NotPermitted("Недостаточно прав для выполнения команды " + commandName+ " для id="+ entityId)
  }

  def checkHasAuthority(commandName:String,roles:Array[String],requiredAuthority: String) {
    debug("user Authorities=" + roles.mkString)
    if(roles.contains(requiredAuthority)) return
    throw new NotPermitted("Недостаточно прав для выполнения команды " + commandName + " требуемая роль=" + requiredAuthority)
  }
}