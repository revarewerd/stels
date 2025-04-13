package ru.sosgps.wayrecall.utils.io;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import ru.sosgps.wayrecall.utils.ScalaConverters;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 09.03.13
 * Time: 13:35
 * To change this template use File | Settings | File Templates.
 */
public class Utils {

    public static void printBytes(byte[] lonbytes) {
        System.out.format(toHexString(lonbytes, " "));
    }

    public static String toHexString(ByteBuf lonbytes, String separator) {
       return toHexString(Unpooled.copiedBuffer(lonbytes).array(),  separator);
    }

    public static String toHexString(byte[] lonbytes, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lonbytes.length; i++) {
            sb.append(String.format("%02x", (lonbytes[i] & 0x000000ff)));
            sb.append(separator);
        }
        return sb.toString();
    }

    public static byte[] asBytesHex(String data) {
        data = data.replaceAll("\\s","");
        byte[] bytes = new byte[data.length() / 2];

        for (int i = 0; i < data.length() - 1; i += 2) {
            byte x = (byte) Integer.parseInt(data.substring(i, i + 2), 16);
            bytes[(i + 1) / 2] = x;
        }
        return bytes;
    }

    public static ByteBuf asByteBuffer(String s) {
       return Unpooled.wrappedBuffer(Utils.asBytesHex(s));
    }

    public static <T> Serializable mapToSerializable(Map<String, T> submitMap) {

        if (submitMap instanceof Serializable)
            return (Serializable) submitMap;

        HashMap<String, Object> result = new HashMap<>(submitMap.size());
        result.putAll(submitMap);
        return result;

    }

    public static <K, V> HashMap<K, V> ensureJavaHashMap(Map<K, V> submitMap) {

        if (submitMap instanceof HashMap)
            return (HashMap<K, V>) submitMap;

        HashMap<K, V> result = new HashMap<>(submitMap.size());
        result.putAll(submitMap);
        return result;

    }

    public static <K, V> HashMap<K, V> ensureJavaHashMap(scala.collection.Map <K, V> submitMap) {
       return  ensureJavaHashMap(ScalaConverters.asJavaMap(submitMap));
    }


}
