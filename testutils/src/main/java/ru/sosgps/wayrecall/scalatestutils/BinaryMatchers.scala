package ru.sosgps.wayrecall.scalatestutils

import org.scalatest.matchers.{MatchResult, Matcher}
import ru.sosgps.wayrecall.utils.io.ByteArrayOps

/**
  * Created by nmitropo on 19.8.2016.
  */
trait BinaryMatchers {

  class ByteArrayInHexMatcher(expected: Array[Byte]) extends Matcher[Array[Byte]] {

    def apply(actual: Array[Byte]) = {
      MatchResult(
        actual sameElements expected,
        s"""array "${actual.toHexString}" did not match expected "${expected.toHexString}"""",
        s"""array "${actual.toHexString}" matches "${expected.toHexString}""""
      )
    }
  }

  def matchInHex(expected: Array[Byte]) = new ByteArrayInHexMatcher(expected)
}

object BinaryMatchers extends BinaryMatchers