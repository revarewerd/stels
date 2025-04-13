package ru.sosgps.wayrecall.utils.io;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 11.06.13
 * Time: 18:11
 * To change this template use File | Settings | File Templates.
 */
public class CRC16Test {

    @Test
    public void test1(){


        final byte[] bytes = Utils.asBytesHex("406366675f7374732330310d0a");

        final int l = (int) CRC16CCITT.invertedKermit(bytes);
        System.out.println(String.format("CRC=%x", l));
        Assert.assertEquals(0xb363,l);


    }


}
