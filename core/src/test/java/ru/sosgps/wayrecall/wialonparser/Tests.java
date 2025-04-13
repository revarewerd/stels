/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.sosgps.wayrecall.wialonparser;

import java.io.*;
import java.text.ParseException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import ru.sosgps.wayrecall.utils.io.Utils;

import static ru.sosgps.wayrecall.wialonparser.WialonParser.parsePackage;


/**
 * @author nickl
 */
public class Tests {

    @Test
    public void test1() throws IOException, ParseException {
        String src = "74000000333533393736303133343435343835004B0BFB70000000030BBB000000270102706F73696E666F00A027AFDF5D9848403AC7253383DD4B400000000000805A40003601460B0BBB0000001200047077725F657874002B8716D9CE973B400BBB00000011010361766C5F696E707574730000000001";


        byte[] bytes = Utils.asBytesHex(src);
        Assert.assertTrue(src.equalsIgnoreCase(Utils.toHexString(bytes, "")));


        InputStream is = new ByteArrayInputStream(bytes);
        WialonPackage pack = parsePackage(is);
        Assert.assertArrayEquals(bytes, pack.rawData);

        Assert.assertEquals("353976013445485", pack.imei);
        Assert.assertEquals(116, pack.size);
        Assert.assertEquals(1259076464000L, pack.time.getTime());
        Assert.assertEquals(3, pack.flags);

        WialonCoordinatesBlock coord = pack.getCoordinatesBlock();
        Assert.assertEquals("posinfo", coord.name);
        Assert.assertEquals(null, coord.value);
        Assert.assertEquals(3003, coord.type);
        Assert.assertEquals(326, coord.course);
        Assert.assertEquals(106.0, coord.height, 0.000001);
        Assert.assertEquals(55.7305664, coord.lat, 0.0001);
        Assert.assertEquals(49.1903648, coord.lon, 0.0001);
        Assert.assertEquals(11, coord.satelliteNum);
        Assert.assertEquals(54, coord.speed);
        Assert.assertEquals(2, coord.dataType);

        List<WialonPackageBlock> blocks = pack.blocks;
        Assert.assertEquals(blocks.get(0), coord);

        WialonPackageBlock block1 = blocks.get(1);
        Assert.assertEquals("pwr_ext", block1.name);
        Assert.assertEquals(3003, block1.type);
        Assert.assertEquals(4, block1.dataType);
        Assert.assertEquals(27.593, block1.value);

        WialonPackageBlock block2 = blocks.get(2);
        Assert.assertEquals("avl_inputs", block2.name);
        Assert.assertEquals(3003, block2.type);
        Assert.assertEquals(3, block2.dataType);
        Assert.assertEquals(1, block2.value);

        System.out.println(pack);

    }

    public static void main(String[] args) throws IOException {

        InputStream is = new BufferedInputStream(
                new FileInputStream("/home/nickl/forStels/data/withoutDoubling/readData1335443286095.wrp")
        );

        PrintWriter writer = new PrintWriter("log1.txt");

        try {

            while (is.available() > 0) {

                writer.println(parsePackage(is));

            }
        } finally {
            is.close();
            writer.close();
        }


    }
}
