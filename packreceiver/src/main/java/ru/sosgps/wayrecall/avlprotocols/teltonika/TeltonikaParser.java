package ru.sosgps.wayrecall.avlprotocols.teltonika;

import ru.sosgps.wayrecall.core.GPSData;
import ru.sosgps.wayrecall.utils.io.CRC16;
import ru.sosgps.wayrecall.utils.io.CRC16CCITT;
import ru.sosgps.wayrecall.utils.io.RichDataInput;
import ru.sosgps.wayrecall.utils.io.Utils;

import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 09.03.13
 * Time: 13:52
 * To change this template use File | Settings | File Templates.
 */
public class TeltonikaParser {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TeltonikaParser.class);

    public static String readIMEI(RichDataInput in) throws IOException {
        int size = in.readShort();
        log.trace("size=" + size);
        return new String(in.readNumberOfBytes(size));
    }

    public static List<GPSData> processPackage(RichDataInput in, String imei) throws IOException {

        int size = readPrefix(in);
        log.trace("size = " + size);
        List<GPSData> gpsDatas = readAVLDataArray(in, imei);
        int crc = readCRC(in);
        log.trace("crc16 = " + crc);
        return gpsDatas;

    }

    public static int readCRC(RichDataInput in) throws IOException {
        return in.readInt();
    }

    public static int readPrefix(RichDataInput in) throws IOException {
        if (in.readInt() != 0)  //Чтение первых нулевых байтов
            throw new IllegalArgumentException("illegal first bytes");

        return in.readInt();
    }

    public static List<GPSData> readAVLDataArray(RichDataInput in, String imei) throws IOException {
        //AVL Data Array
        byte codec = in.readByte();
        log.trace("codec = " + codec);

        byte numberOfRecords = in.readByte();
        log.trace("numberOfRecords = " + numberOfRecords);

        ArrayList<GPSData> gpsDatas = new ArrayList<GPSData>(numberOfRecords);
        for (int i = 0; i < numberOfRecords; i++) {
            log.trace("record" + i + ":");
            log.trace("");
            gpsDatas.add(readRecord(in, imei));
            log.trace("");
            log.trace("");
        }
        byte numberOfRecords2 = in.readByte();
        if (numberOfRecords2 != numberOfRecords)
            throw new IllegalArgumentException("numbers OfRecords does not match");

        return gpsDatas;
    }

    private static GPSData readRecord(RichDataInput in, String imei) throws IOException {
        //AVL data
        long timestamp = in.readLong();
        Date time = new Date(timestamp);
        log.trace("timestamp = " + time);
        byte priority = in.readByte();
        log.trace("priority = " + priority);

        //GPS Element
        double lon = in.readInt() / 10000000.0;
        log.trace("lon = " + lon);
        double lat = in.readInt() / 10000000.0;
        log.trace("lat = " + lat);

        int altitude = in.readUnsignedShort();
        log.trace("altitude = " + altitude);
        short angle = in.readShort();
        log.trace("angle = " + angle);

        byte satellites = in.readByte();
        log.trace("satellites = " + satellites);
        short speed = in.readShort();
        log.trace("speed = " + speed);

        GPSData gpsData = new GPSData(null, imei, lon, lat, time, speed, angle, satellites);
        gpsData.data.put("protocol", "Teltonika");
        gpsData.data.put("alt", Double.valueOf(altitude));
        //IO element

        byte eventIOId = in.readByte();
        log.trace("eventIOId = " + eventIOId);
        byte numberOfTotalIO = in.readByte();
        log.trace("numberOfTotalIO = " + numberOfTotalIO);
        byte numberOf1ByteIO = in.readByte();
        log.trace("numberOf1ByteIO = " + numberOf1ByteIO);

        for (int i = 0; i < numberOf1ByteIO; i++) {
            int id = in.readUnsignedByte();
            int value = in.readUnsignedByte();
            log.trace(i + " id = " + id + " value = " + value);
            gpsData.data.put(String.valueOf(id), value);
        }

        byte numberOf2ByteIO = in.readByte();
        log.trace("numberOf2ByteIO = " + numberOf2ByteIO);
        for (int i = 0; i < numberOf2ByteIO; i++) {
            int id = in.readUnsignedByte();
            int value = in.readUnsignedShort();
            log.trace(i + " id = " + id + " value = " + value);
            gpsData.data.put(String.valueOf(id), value);
        }

        byte numberOf4ByteIO = in.readByte();
        log.trace("numberOf4ByteIO = " + numberOf4ByteIO);
        for (int i = 0; i < numberOf4ByteIO; i++) {
            int id = in.readUnsignedByte();
            int value = in.readInt();
            log.trace(i + " id = " + id + " value = " + value);
            gpsData.data.put(String.valueOf(id), value);
        }

        byte numberOf8ByteIO = in.readByte();
        log.trace("numberOf8ByteIO = " + numberOf8ByteIO);
        for (int i = 0; i < numberOf8ByteIO; i++) {
            int id = in.readUnsignedByte();
            long value = in.readLong();
            log.trace(i + " id = " + id + " value = " + value);
            gpsData.data.put(String.valueOf(id), value);
        }

        final Object pwrLevel = gpsData.data.get("66");
        if (pwrLevel != null)
            gpsData.data.put("pwr_ext", ((Number) pwrLevel).intValue() / 1000.0);

        return gpsData;
    }

    public static void main(String[] args) throws IOException {

        String imeiBytes = "000F313233343536373839303132333435";

        RichDataInput in = new RichDataInput(new ByteArrayInputStream(Utils.asBytesHex(imeiBytes)));

        String imeistr = readIMEI(in);
        System.out.println("imeistr = " + imeistr);


        String data = "080400000113fc208dff000f14f650209cca80006f00d60400040004030101150" +
                "316030001460000015d0000000113fc17610b000f14ffe0209cc580006e00c005" +
                "00010004030101150316010001460000015e0000000113fc284945000f150f002" +
                "09cd200009501080400000004030101150016030001460000015d0000000113fc" +
                "267c5b000f150a50209cccc000930068040000000403010115001603000146000" +
                "0015b0004";

        byte[] bytes = Utils.asBytesHex(data);

        Utils.printBytes(bytes);
        System.out.println("");

        System.out.println("calkedCrc1 = " + String.format("%x", CRC16.calc(bytes)));
        System.out.println("calkedCrc2 = " + String.format("%x", CRC16CCITT.calc(bytes)));
        System.out.println("calkedCrc3 = " + String.format("%x", CRC16CCITT.invertedKermit(bytes)));

        RichDataInput in1 = new RichDataInput(bytes);
        List<GPSData> gpsDatas = readAVLDataArray(in1, imeistr);

        for (GPSData gpsData : gpsDatas) {
            System.out.println(gpsData.toString());
        }

    }


}
