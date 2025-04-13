package ru.sosgps.wayrecall.scalatestutils

import java.net.Socket

import org.scalatest.{BeforeAndAfterEach, Suite}
import ru.sosgps.wayrecall.core.MongoDBManager

/**
  * Created by nickl-mac on 11.06.16.
  */
trait CleanableMdbm /*extends BeforeAndAfterEach with grizzled.slf4j.Logging*/ {
  //this: Suite =>

  def mdbm: MongoDBManager

  def inClearDb[T](func : => T):T= {
    mdbm.getDatabase().dropDatabase()
    func
  }

//  override def beforeEach() {
//    debug("before each")
//    mdbm.getDatabase().dropDatabase()
//    super.beforeEach()
//  }
//
//
//  override def afterEach() {
//    try {
//      super.afterEach()
//    }
//    finally {
//
//      debug("afterEach ")
//    }
//  }

}
