package ru.sosgps.wayrecall.web

import org.junit.Assert._
import org.junit.Test
import ru.sosgps.wayrecall.monitoring.web.{DigioutCommand, FMBDigioutCommand, TeltonikaDigioutCommand}
import ru.sosgps.wayrecall.sms.SMS

/**
 * Created by nickl on 23.12.13.
 */
class TeltonikaDigioutCommandTest {

  @Test
  def testTeltonikaDigioutCommand() {
    def testResp(block: Boolean, mess: String) =
      runTest(block, mess, new TeltonikaDigioutCommand("u", "u", "p", _))

    testResp(true, "Digital Outputs are set to: 11")
    testResp(false, "Digital Outputs are set to: 00")

    testResp(true, "Digital Outputs are set to: 11. Timeouts are: 0 0")
    testResp(false, "Digital Outputs are set to: 00. Timeouts are: 0 0")
  }

  @Test
  def testFMBDigioutCommand() {
    def testResp(block: Boolean, mess: String) =
      runTest(block, mess, new FMBDigioutCommand("u", "u", "p", _))

    testResp(true, "DOUT1:1 Timeout:INFINITY DOUT2:1 Timeout:INFINITY")
    testResp(false, "DOUT1:0 Timeout:INFINITY DOUT2:0 Timeout:INFINITY")

    testResp(true, "DOUT1:1 Timeout:INFINITY")
    testResp(false, "DOUT1:0 Timeout:INFINITY")
  }


  private def runTest(block: Boolean, mess: String, command1: Boolean => DigioutCommand) {
    val command = command1(block)
    command.acceptResponse(new SMS(-1, mess, "0"))
    assertEquals(s"answer '$mess' should be considered as ${if (block) "" else "un"}blocking", Some(block), command.blocked)
  }
}
