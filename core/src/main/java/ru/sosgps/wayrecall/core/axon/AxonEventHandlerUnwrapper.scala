package ru.sosgps.wayrecall.core.axon

import org.axonframework.eventhandling.EventListener
import org.springframework.aop.IntroductionInfo
import org.springframework.aop.framework.Advised
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor

/**
  * Created by nmitropo on 3.10.2016.
  * Сделан для unwrap-a proxy, которое зачем-то создает [[org.axonframework.common.annotation.AbstractAnnotationHandlerBeanPostProcessor]]
  *
  * ``После апгрейда до Spring 4 это стало проблемой так как спринг перестал выполнять конструкторы при проксировании``
  */
class AxonEventHandlerUnwrapper extends DestructionAwareBeanPostProcessor with grizzled.slf4j.Logging {
  override def requiresDestruction(bean: scala.Any): Boolean = false

  override def postProcessBeforeDestruction(bean: scala.Any, beanName: String): Unit = {}

  override def postProcessAfterInitialization(bean: scala.Any, beanName: String): AnyRef = {
    //debug(s"postProcessAfterInitialization: $beanName : $bean ${bean.getClass}")
    bean match {
      case advised: Advised if advised.getAdvisors.exists {
          case e: IntroductionInfo => e.getInterfaces.contains(classOf[EventListener])
          case _ => false
      } => advised.getTargetSource.getTarget
      case els => els.asInstanceOf[AnyRef]
    }
  }

  override def postProcessBeforeInitialization(bean: scala.Any, beanName: String): AnyRef = bean.asInstanceOf[AnyRef]
}
