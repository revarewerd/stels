package ru.sosgps.wayrecall.avlprotocols.ruptela;

import ru.sosgps.wayrecall.utils.io.Utils;

/**
* Created with IntelliJ IDEA.
* User: nickl
* Date: 07.06.13
* Time: 0:42
* To change this template use File | Settings | File Templates.
*/
public class RuptelaIncomingPackage {
    public String imei;
    public byte commandId;
    public byte[] dataBytes;

    @Override
    public String toString() {
        return "RuptelaIncomingPackage{" +
                "imei='" + imei + '\'' +
                ", commandId=" + commandId +
                ", dataBytes=[" + Utils.toHexString(dataBytes," ") +
                "]}";
    }
}
