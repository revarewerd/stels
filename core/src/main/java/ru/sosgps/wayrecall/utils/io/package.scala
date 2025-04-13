package ru.sosgps.wayrecall.utils

import java.io._
import com.mongodb.casbah.Imports._
import org.bson.BasicBSONDecoder
import com.mongodb.util.JSONCallback
import scala.Serializable
import java.nio.file.{Files, Paths, Path}


package object io extends grizzled.slf4j.Logging{

  def iterateStream[S <: Closeable, T](stream: S, readitem: S => T): Stream[T] = {
    try {
      val dbo = readitem(stream)
      dbo #:: iterateStream(stream, readitem)
    }
    catch {
      case e: Exception => {
        stream.close()
        Stream.empty
      }
    }
  }

  def serialize(obj: Serializable): Array[Byte] = {
    val bos = new ByteArrayOutputStream()
    val out = new ObjectOutputStream(bos)
    try {
      out.writeObject(obj)
      bos.toByteArray()
    } finally {
      out.close()
      bos.close()
    }
  }

  def deserialize[T](data: Array[Byte]): T = {
    val bos = new ByteArrayInputStream(data)
    val out = new ObjectInputStream(bos)
    try {
      out.readObject().asInstanceOf[T]
    } finally {
      out.close()
      bos.close()
    }
  }

  def tryDeserialize[T](data: Array[Byte]): Either[Throwable, T] = {
    scala.util.control.Exception.catching(classOf[Exception]).either(deserialize[T](data))
  }

  def backupPrev(file: File): File = {
    file.renameTo(newFile(file.getPath + ".backup"))
    file
    //Files.copy(file.toPath,file.toPath. )
  }

  def newFile(name: String): File = {
    val i = name.indexOf("{n}")
    val (pref, dot, suff) = if(i == -1)
      (name,".","")
    else
      (name.substring(0, i),".", name.substring(i+3, name.length))
    ((pref + suff) #:: Stream.from(1).map(pref +dot+_+suff)).map(name => new File(name)).filter(!_.exists()).head
  }

  implicit class ByteArrayOps(val array: Array[Byte]) extends AnyVal {

    def toHexString: String = Utils.toHexString(array, " ")

  }

}
