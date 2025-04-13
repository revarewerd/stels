package ru.sosgps.wayrecall.core

/**
 * Created by IVAN on 18.06.2014.
 */
trait CommandEntityInfo {
  def getEntity(): String
  def getEntityId(): Any
}
