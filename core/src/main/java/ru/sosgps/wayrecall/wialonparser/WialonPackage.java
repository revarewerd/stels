/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.sosgps.wayrecall.wialonparser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author nickl
 */
public class WialonPackage {

    public int size;
    public String imei;
    public Date time;
    public int flags;
    public List<WialonPackageBlock> blocks = new ArrayList<WialonPackageBlock>(5);
    public byte[] rawData = null;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WialonPackage");
        sb.append(" size:").append(size);
        sb.append(" imei:").append(imei);
        sb.append(" time:").append(time);
        sb.append(" flags:").append(flags);

        for (WialonPackageBlock wialonPackageBlock : blocks) {
            sb.append("\n  ").append(wialonPackageBlock);
        }

        return sb.toString();
    }

    public WialonCoordinatesBlock getCoordinatesBlock() {

        for (WialonPackageBlock wialonPackageBlock : blocks) {
            if (wialonPackageBlock instanceof WialonCoordinatesBlock) {
                return (WialonCoordinatesBlock) wialonPackageBlock;
            }
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WialonPackage that = (WialonPackage) o;

        if (flags != that.flags) return false;
        if (size != that.size) return false;
        if (blocks != null ? !blocks.equals(that.blocks) : that.blocks != null) return false;
        if (imei != null ? !imei.equals(that.imei) : that.imei != null) return false;
        if (time != null ? !time.equals(that.time) : that.time != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = size;
        result = 31 * result + (imei != null ? imei.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        result = 31 * result + flags;
        return result;
    }
}
