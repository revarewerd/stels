package ru.sosgps.wayrecall.utils


import org.apache.poi.ss.usermodel.{Cell, RichTextString}
import java.util.{Calendar, Date}

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 01.05.13
 * Time: 15:02
 * To change this template use File | Settings | File Templates.
 */
object POIUtils {


  implicit def toTypedValue(cell: Cell) = new {

    def setTypedValue(a: Any) = {
      a match {
        case d: Date => cell.setCellValue(d)
        case c: Calendar => cell.setCellValue(c)
        case a: Boolean => cell.setCellValue(a)
        case r: RichTextString => cell.setCellValue(r)
        case s: String => cell.setCellValue(s);
        case d: Double => cell.setCellValue(d)
        case d: Int => cell.setCellValue(d)
        case d: Long => cell.setCellValue(d)
        case d: Short => cell.setCellValue(d)
        case d: Float => cell.setCellValue(d)
        case d: java.lang.Number => cell.setCellValue(d.doubleValue())
        case a: AnyRef => cell.setCellValue(a.toString)

      }
    }

    def getTypedValue(): Any = {
      cell.getCellType match {
        case Cell.CELL_TYPE_NUMERIC => cell.getNumericCellValue
        case Cell.CELL_TYPE_STRING => cell.getStringCellValue
        case Cell.CELL_TYPE_FORMULA => cell.getCellFormula
        case Cell.CELL_TYPE_BLANK => ""
        case Cell.CELL_TYPE_BOOLEAN => cell.getBooleanCellValue
        case Cell.CELL_TYPE_ERROR => cell.getErrorCellValue
      }
    }

  }

}
