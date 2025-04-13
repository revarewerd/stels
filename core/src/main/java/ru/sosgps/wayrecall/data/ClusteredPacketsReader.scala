package ru.sosgps.wayrecall.data

import scala.concurrent.Future

/**
 * Created by nickl on 08.04.15.
 */
// Но вообще есть подозрения что этот класс можно заменить на PackagesStore по аналогии с DbMappingPacketsWriter
trait ClusteredPacketsReader {

  @throws(classOf[IllegalImeiException])
  def getStoreByImei(imei: String): PackagesStore

  def uidByImei(imei: String): Option[String]

  //TODO: refactor this
  @deprecated("this is ugly hack must bee removed soon")
  def futureOnImei[T](imei: String)(f: => T): Future[T] = try (Future.successful(f)) catch {case e: Exception => Future.failed(e)}
}
