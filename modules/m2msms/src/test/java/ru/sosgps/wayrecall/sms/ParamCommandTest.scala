package ru.sosgps.wayrecall.sms

import org.junit.{Assert, Test}

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 10.04.13
 * Time: 14:59
 * To change this template use File | Settings | File Templates.
 */
class ParamCommandTest {


  @Test
  def text1() {

    val command = new TeltonikaParamCommand("login", "password", "107", "180");


    Assert.assertTrue(command.acceptResponse(new SMS(0, "Param ID:107 New Val:180", true, "", "", null, null, null)))
    Assert.assertTrue(command.acceptResponse(new SMS(0, "Param ID:107 Val:180", true, "", "", null, null, null)))
    Assert.assertTrue(command.acceptResponse(new SMS(0, "Param ID:107 Text:180", true, "", "", null, null, null)))
    Assert.assertTrue(command.acceptResponse(new SMS(0, "Param ID:107 New Text:180", true, "", "", null, null, null)))
    Assert.assertFalse(command.acceptResponse(new SMS(0, "Param ID:107 NewText:180", true, "", "", null, null, null)))
    Assert.assertFalse(command.acceptResponse(new SMS(0, "Param ID:108 New Text:180", true, "", "", null, null, null)))
    Assert.assertTrue(command.acceptResponse(new SMS(0, "Param ID:107 New Text:ggg", true, "", "", null, null, null)))
  }


}
