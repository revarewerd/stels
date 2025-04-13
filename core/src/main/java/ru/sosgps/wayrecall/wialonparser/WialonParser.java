/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.sosgps.wayrecall.wialonparser;

import ru.sosgps.wayrecall.utils.io.RichDataInput;

import java.io.*;
import java.util.Date;

/**
 * @author nickl-new
 */
public class WialonParser {

    public static WialonPackage parsePackage(InputStream is) throws IOException {
        return parsePackage((DataInput) new DataInputStream(is));
    }

    public static WialonPackage parsePackage(DataInput ds0) throws IOException {
        RichDataInput ds = RichDataInput.fromDataInput(ds0);
        byte[] sizebytes = ds.readNumberOfBytes(4);

        int size = sizebytes[3] << 24
                | sizebytes[2] << 16
                | sizebytes[1] << 8
                | sizebytes[0] & 0x000000ff;


        //System.out.println("size=" + size);
        //printBytes(sizebytes);   

        byte[] body = new byte[size];

        ds.readFully(body);

        ByteArrayInputStream packageBody = new ByteArrayInputStream(body);
        WialonPackage result = parsePackageBody(size, new DataInputStream(packageBody));

        byte[] rawdata = new byte[sizebytes.length + body.length];

        //System.out.println("rawdata len= "+ rawdata.length);

        for (int i = 0; i < sizebytes.length; i++) {
            rawdata[i] = sizebytes[i];
        }

        for (int i = sizebytes.length; i < rawdata.length; i++) {
            rawdata[i] = body[i - sizebytes.length];
        }

        result.rawData = rawdata;

        return result;
    }

    private static WialonPackage parsePackageBody(int size, DataInput dss) throws IOException {

        RichDataInput ds = RichDataInput.fromDataInput(dss);

        WialonPackage wp = new WialonPackage();

        wp.size = size;

        byte[] bsba = ds.readNullTerminated();
        wp.imei = new String(bsba);

        int seconds = ds.readInt();

        wp.time = new Date(((long) seconds) * 1000);

        wp.flags = ds.readInt();

        WialonPackageBlock rblock;
        while ((rblock = parseBlock(ds)) != null) {
            wp.blocks.add(rblock);
        }

        return wp;
    }

    private static WialonPackageBlock parseBlock(RichDataInput ds) throws IOException {
        short blocktype;
        try {
            blocktype = ds.readShort();
        } catch (EOFException e) {
            return null;
        }
        int blockSize = ds.readInt();
        boolean hidden = ds.readByte() == 1;
        byte blockDataType = ds.readByte();


        String blockname = new String(ds.readNullTerminated());

        if (blockname.equals("posinfo")) {
            WialonCoordinatesBlock block = new WialonCoordinatesBlock();
            block.name = blockname;
            block.type = blocktype;
            block.size = blockSize;
            block.hidden = hidden;
            block.dataType = blockDataType;

            block.lon = ds.longToDouble();
            block.lat = ds.longToDouble();
            block.height = ds.longToDouble();
            block.speed = ds.readShort();
            block.course = ds.readShort();
            while(block.course < 0)
                block.course += 360;

            block.satelliteNum = ds.readByte();

            return block;
        } else {
            WialonPackageBlock block = new WialonPackageBlock();
            block.name = blockname;
            block.type = blocktype;
            block.size = blockSize;
            block.hidden = hidden;
            block.dataType = blockDataType;

            if (blockDataType == 0x1) // Текстовый
            {
                block.value = new String(ds.readNullTerminated());
            } else if (blockDataType == 0x2) // Binary
            {
                byte[] binarydata = new byte[blockSize - 2];
                ds.readFully(binarydata);
                block.value = binarydata;
            } else if (blockDataType == 0x3) // Integer                 
            {
                block.value = ds.readInt();

            } else if (blockDataType == 0x4) // double                 
            {
                block.value = ds.longToDouble();
            } else if (blockDataType == 0x5) //long                
            {
                block.value = ds.readLong();
            }


            return block;
        }

    }



}
