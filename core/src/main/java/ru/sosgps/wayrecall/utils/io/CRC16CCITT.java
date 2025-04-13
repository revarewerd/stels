package ru.sosgps.wayrecall.utils.io;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 19.05.13
 * Time: 0:02
 * To change this template use File | Settings | File Templates.
 */
public class CRC16CCITT {

    public static int calc(byte[] bytes) {
        int crc = 0xFFFF;          // initial value
        int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12)

        // byte[] testBytes = "123456789".getBytes("ASCII");


        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff;
        return crc;
    }

    public static long invertedKermit(byte[] val)
    {
        long crc;
        long q;
        byte c;
        crc = 0;
        for (int i = 0; i < val.length; i++)
        {
            c = val[i];
            q = (crc ^ c) & 0x0f;
            crc = (crc >> 4) ^ (q * 0x1081);
            q = (crc ^ (c >> 4)) & 0xf;
            crc = (crc >> 4) ^ (q * 0x1081);
        }

        return crc;
//        long b1 = (crc & 0x00ff00ff) << 8;
//        long b2 = (crc & 0xff00ff00) >> 8;
//        return b1 | b2;
    }

}
