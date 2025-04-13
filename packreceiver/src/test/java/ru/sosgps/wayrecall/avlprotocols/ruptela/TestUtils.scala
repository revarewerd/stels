package ru.sosgps.wayrecall.avlprotocols.ruptela

import java.io.InputStream
import scala.io.{Source, BufferedSource}
import java.util.zip.GZIPInputStream
import ru.sosgps.wayrecall.utils.io.Utils

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 02.11.13
 * Time: 18:58
 * To change this template use File | Settings | File Templates.
 */
private[ruptela] object TestUtils {

  private[ruptela] def getTextResource(file: String): BufferedSource = {
    Source.fromInputStream(
      getResource(file)
    )
  }

  private[ruptela] def getResource(file: String): InputStream = {
    val resourceAsStream = getClass.getResourceAsStream(file)
    if (file.endsWith(".gz"))
      new GZIPInputStream(resourceAsStream)
    else
      resourceAsStream
  }

  private[ruptela] def blioadReader(in: Boolean, source: BufferedSource): Iterator[Array[Byte]] = {
    val lineStart = if (in) "->" else "<-"
    source.getLines().collect(
    {case s if (s.contains(lineStart) && !s.contains("EOF")) => Utils.asBytesHex(s.split(lineStart)(1).replaceAll("\\s", ""))})
  }

}

class ArrayIteratorInputStream(datatosend: Iterator[Array[Byte]]) extends InputStream {
  var activeLine: Array[Byte] = Array.empty

  val byteItr = datatosend.flatMap(a => {activeLine = a; a}).toIterator

  def read() = {
    byteItr.next() & 0x000000ff
  }

  def hasNext = byteItr.hasNext
}
