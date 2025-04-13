package ru.sosgps.wayrecall.packreceiver

import org.junit.Assert
import ru.sosgps.wayrecall.utils.tryNumerics

/**
 * Created by nickl on 27.04.15.
 */
object TestUlits {

  def assertEquals(message: String, expected: Any, actual: Any, permittedRelativeMistake: Double): Unit = {
    val expectdDouble = expected.tryDouble
    Assert.assertEquals(message, expectdDouble, actual.tryDouble, expectdDouble * permittedRelativeMistake)
  }

  def assertEquals(expected: Any, actual: Any, permittedRelativeMistake: Double): Unit = {
    val expectdDouble = expected.tryDouble
    Assert.assertEquals(expectdDouble, actual.tryDouble, expectdDouble * permittedRelativeMistake)
  }

}
