package ru.sosgps.wayrecall.avlprotocols.ruptela;

import com.google.common.base.Charsets;
import ru.sosgps.wayrecall.core.GPSData;
import ru.sosgps.wayrecall.utils.io.CRC16CCITT;
import ru.sosgps.wayrecall.utils.io.RichDataInput;
import ru.sosgps.wayrecall.utils.io.Utils;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 09.03.13
 * Time: 13:52
 * To change this template use File | Settings | File Templates.
 */
public class RuptelaParser {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RuptelaParser.class);


    public static RuptelaIncomingPackage parsePackage(RichDataInput in) throws IOException {

        RuptelaIncomingPackage result = new RuptelaIncomingPackage();
        final short length = in.readShort();
        log.trace("length=" + length);
        final byte[] body = in.readNumberOfBytes(length);
        RichDataInput inbody = new RichDataInput(body);
        result.imei = inbody.readLong() + "";
        log.trace("imei=" + result.imei);
        result.commandId = inbody.readByte();
        log.trace("commandId=" + result.commandId);

        result.dataBytes = inbody.readNumberOfBytes(length - 9);

        final int calckedCRC = (int) CRC16CCITT.invertedKermit(body);

        final int CRC = in.readUnsignedShort();
        log.trace(String.format("CRC=%x expected=%x", CRC, calckedCRC));
        if (calckedCRC != CRC)
            log.warn("CRC does not match " + String.format("CRC=%x(%d) expected=%x(%d)", CRC, CRC, calckedCRC, calckedCRC));
        if (log.isTraceEnabled())
            log.trace("dataBytes = " + Utils.toHexString(result.dataBytes, ""));

        return result;
    }

    public static List<GPSData> processGpsPackage(RichDataInput in) throws IOException {

        RuptelaIncomingPackage p = parsePackage(in);

        return getGpsDatas(p);
    }

    public static List<GPSData> getGpsDatas(RuptelaIncomingPackage p) throws IOException {
        if (p.commandId == 1) {
            return getGpsDatas(p.imei, p.dataBytes);
        }

        throw new UnsupportedOperationException("commandId =" + p.commandId);
    }

    private static List<GPSData> getGpsDatas(String imei, byte[] dataBytes) throws IOException {
        RichDataInput d = new RichDataInput(dataBytes);
        final byte recordsLeftFlag = d.readByte();
        log.trace("recordsLeftFlag=" + recordsLeftFlag);
        final byte numberOfRecords = d.readByte();
        log.trace("numberOfrecords=" + numberOfRecords);

        List<GPSData> result = new ArrayList<>(numberOfRecords);

        for (int i = 0; i < numberOfRecords; i++) {
            result.add(readRecord(imei, d));
        }

        // assert all data was read
        try {
            d.readByte();
            log.warn("data remains in pack");
        } catch (EOFException e) {
        }

        return result;
    }

    private static GPSData readRecord(String imei, RichDataInput d) throws IOException {
        final int timestamp = d.readInt();
        final byte virtualmills = d.readByte();
        final Date time = new Date(timestamp * 1000L + virtualmills);
        log.trace("time=" + time);
        d.readByte(); // reserved
        final double lon = ((double) d.readInt()) / 10000000;
        final double lat = ((double) d.readInt()) / 10000000;
        log.trace("lon=" + lon + " lat=" + lat);
        final double alt = ((double) d.readUnsignedShort()) / 10;
        log.trace("alt=" + alt);
        final int angle = d.readUnsignedShort() / 100;
        log.trace("angle=" + angle);
        final byte satll = d.readByte();
        log.trace("satll=" + satll);
        final short speed = d.readShort();
        log.trace("speed=" + speed);
        final byte hdop = d.readByte();
        log.trace("hdop=" + hdop);

        final GPSData gpsData = new GPSData(null, imei, lon, lat, time, speed, (short) angle, satll);
        gpsData.data.put("protocol", "Ruptela");
        gpsData.data.put("alt", Double.valueOf(alt));
        //IO element

        byte eventIOId = d.readByte();
        log.trace("eventIOId = " + eventIOId);
        byte numberOf1ByteIO = d.readByte();
        log.trace("numberOf1ByteIO = " + numberOf1ByteIO);

        for (int i = 0; i < numberOf1ByteIO; i++) {
            int id = d.readUnsignedByte();
            int value = d.readUnsignedByte();
            log.trace(i + " id = " + id + " value = " + value);
            gpsData.data.put(String.valueOf(id), value);
        }

        byte numberOf2ByteIO = d.readByte();
        log.trace("numberOf2ByteIO = " + numberOf2ByteIO);
        for (int i = 0; i < numberOf2ByteIO; i++) {
            int id = d.readUnsignedByte();
            int value = d.readUnsignedShort();
            log.trace(i + " id = " + id + " value = " + value);
            gpsData.data.put(String.valueOf(id), value);
        }

        byte numberOf4ByteIO = d.readByte();
        log.trace("numberOf4ByteIO = " + numberOf4ByteIO);
        for (int i = 0; i < numberOf4ByteIO; i++) {
            int id = d.readUnsignedByte();
            int value = d.readInt();
            log.trace(i + " id = " + id + " value = " + value);
            gpsData.data.put(String.valueOf(id), value);
        }

        byte numberOf8ByteIO = d.readByte();
        log.trace("numberOf8ByteIO = " + numberOf8ByteIO);
        for (int i = 0; i < numberOf8ByteIO; i++) {
            int id = d.readUnsignedByte();
            long value = d.readLong();
            log.trace(i + " id = " + id + " value = " + value);
            gpsData.data.put(String.valueOf(id), value);
        }

        final Object pwrLevel = gpsData.data.get("29");
        if (pwrLevel != null)
            gpsData.data.put("pwr_ext", ((Number) pwrLevel).intValue() / 1000.0);

        return gpsData;
    }


    public static byte[] recordACKcommand(boolean positive) throws IOException {
        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream(2);
        DataOutputStream d = new DataOutputStream(bodyStream);
        d.writeByte(100);
        if (positive)
            d.writeByte(1);
        else
            d.writeByte(0);
        d.close();

        return packBody(bodyStream.toByteArray());
    }

    public static byte[] deviceConfigurationCommand(String command) throws IOException {
        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream(1 + command.length());
        DataOutputStream d = new DataOutputStream(bodyStream);
        d.writeByte(102);
        d.writeBytes(command);
        d.close();

        return packBody(bodyStream.toByteArray());
    }

    public static byte[] packBody(byte[] body) throws IOException {
        final int length = body.length;
        final int crc = (int) CRC16CCITT.invertedKermit(body);

        ByteArrayOutputStream result = new ByteArrayOutputStream(4 + length);
        DataOutputStream rd = new DataOutputStream(result);
        rd.writeShort(length);
        rd.write(body, 0, length);
        rd.writeShort(crc);
        rd.close();
        return result.toByteArray();
    }

    public static void main1(String[] args) throws IOException {

        String data = "002A00000B1A29F64B1A010001" +
                "4D086913" +
                "00 " +
                "00" +
                "0F10E910" +
                "209B165B" +
                "0082" +
                "5208" +
                "06" +
                "0000" +
                "13" +
                "00" +
                "02" +
                "04" +
                "01" +
                "05" +
                "01" +
                "00" +
                "00" +
                "00" +
                "ADE9";

        System.out.println(log.isTraceEnabled());
        System.out.println("datasize=" + data.length());

        byte[] bytes = Utils.asBytesHex(data);

        final List<GPSData> gpsDatas = processGpsPackage(new RichDataInput(bytes));

        System.out.println("gpsDatas=" + gpsDatas);

        System.out.println("positive=" + Utils.toHexString(recordACKcommand(true), ""));
        System.out.println("negative=" + Utils.toHexString(recordACKcommand(false), ""));


        final long l = CRC16CCITT.invertedKermit(Utils.asBytesHex("00000B1A2A151117" +
                "03353030312C30302E30312E30332C313536392C33312C31"));
        System.out.println("ff=" + l + String.format("  %x", l));

    }

    public static void main(String[] args) throws IOException {


        //String data = "00 02 64 01 13 bc 00 0c 68 7c 46 55 5f 53 54 52 54 2a 0d 0a b6 6b".replaceAll("\\s","");
        //String data = "00 02 64 01 13 bc 00 0e 66 23 63 66 67 5f 73 74 61 72 74 40 0d 0a 09 47".replaceAll("\\s","");
        String data = "001600000c076b8b337e02406366675f7374732331300d0a840f".replaceAll("\\s", "");
//        String data = "02 0d 66 23 63 66 67 5f 73 65 6e 64 40 00 02 01 24 00 64 00 01 00 65 00 20 69 6e 74 65 72 6e 65 74 2e 6d 74 " +
//                "73 2e 72 75 00 05 00 03 00 24 01 08 00 00 00 00 00 94 cd fa 68 66 00 20 6d 74 73 00 11 d5 0a 3a 0b " +
//                "00 02 00 38 01 08 00 dc d2 4d 77 \n" +
//                "00 00 00 00 00 00 00 00 00 00 00 00 67 00 20 6d 74 73 00 11 d5 0a 3a 0b 00 02 00 38 01 08 00 dc d2 " +
//                "4d 77 00 00 00 00 00 00 00 00 00 00 00 00 6e 00 28 39 31 2e 32 33 30 2e 31 35 31 2e 33 33 00 \n" +
//                "65 00 05 00 03 00 24 01 08 00 00 00 00 00 94 cd fa 68 78 16 1b 00 f0 f2 1d 00 6f 00 28 00 00 00 00 " +
//                "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 \n" +
//                "00 00 00 00 00 78 00 02 81 23 79 00 02 00 00 7a 00 02 3c 00 82 00 10 00 00 00 00 00 00 00 00 00 00 " +
//                "00 00 00 00 00 00 83 00 10 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 84 00 10 00 00 00 \n" +
//                "00 00 00 00 00 00 00 00 00 00 00 00 00 85 00 10 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 86 " +
//                "00 10 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 87 00 10 00 00 00 00 00 00 00 00 00 00 \n" +
//                "00 00 00 00 00 00 88 00 10 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 89 00 10 00 00 00 00 00 " +
//                "00 00 00 00 00 00 00 00 00 00 00 8a 00 10 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 8b \n" +
//                "00 10 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 8c 00 10 35 38 30 33 00 04 19 04 0a 00 02 00 " +
//                "53 01 0c 00 8d 00 10 00 2a 19 00 00 00 00 00 02 00 02 00 7a 01 08 00 c8 00 01 00 c9 00 01 00 \n" +
//                "ca 00 04 00 00 00 00 cb 00 04 00 00 00 00 d2 00 01 00 dc 00 01 00 e6 00 01 00 2c 01 01 01 90 01 01 00 91 01 02 00 00 92 01 02 00 00 93 01 01 00 94 01 01 00 95 01 01 1e 96 01 02 00 00 0d 0a c1 \n" +
//                "d5".replaceAll("\\s", "");

        byte[] bytes = Utils.asBytesHex(data);

        RichDataInput in = new RichDataInput(bytes);

        while (true) {
            short len = in.readShort();
            System.out.println("len = " + len);
//            final byte commandId = in.readByte();
//            System.out.println("commandId=" + commandId);
//            byte[] body = in.readNumberOfBytes(len-1);

            final String imei = in.readLong() + "";
            System.out.println("imei=" + imei);
            final byte commandId = in.readByte();
            System.out.println("commandId=" + commandId);
            byte[] body = in.readNumberOfBytes(len - 9);
            System.out.println("@cfg_sts# =" + Utils.toHexString("@cfg_sts#".getBytes(Charsets.US_ASCII), " "));
            System.out.println("body = " + Utils.toHexString(body, " ") + " -> " + new String(body,
                    Charsets.US_ASCII));
            short crc = in.readShort();
        }


    }


}

