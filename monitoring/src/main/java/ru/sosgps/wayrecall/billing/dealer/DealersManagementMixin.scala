package ru.sosgps.wayrecall.billing.dealer

import javax.annotation.Resource

import org.springframework.beans.factory.annotation.Value
import ru.sosgps.wayrecall.core.InstanceConfig
import ru.sosgps.wayrecall.initialization.{MultiserverConfig, MultiDbManager}

import scala.beans.BeanProperty

import com.mongodb.casbah.Imports._

import ru.sosgps.wayrecall.utils.{tryNumerics, ExtDirectService, typingMapJava}

/**
 * Created by nickl on 20.12.14.
 */
trait DealersManagementMixin {

  @BeanProperty
  @Value("${instance.name}")
  var instanceName: String = null

  @Resource(name = "multiDbManager")
  var mmdbm: MultiDbManager = null

  @Resource(name = "multiserverConfig")
  var mscfg: MultiserverConfig = null

  protected def subDealers(): Iterable[InstanceConfig] = {
    mscfg.instances.filter(
      _.properties.getAs[String]("instance.parent") match {
        case Some(parent) => parent == instanceName
        case None => false
      }
    )
  }

}
