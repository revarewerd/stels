package ru.sosgps.wayrecall.core;

import ru.sosgps.wayrecall.data.sleepers.LBS;

import java.util.Date;

/**
 * Created by nickl on 21.01.14.
 */
public class LBSData {

    /**
     * Идентификатор объекта, к которому данное устройство приписано
     */
    public String uid;
    /**
     * Идетификатор устройства, который был прислан вместе с пакетом.
     */
    public String imei;
    public LBS lbs;
    public Date time;

    public LBSData(String uid, String imei, LBS lbs, Date time) {
        this.uid = uid;
        this.imei = imei;
        this.lbs = lbs;
        this.time = time;
    }

    @Override
    public String toString() {
        return "LBSData{" +
                "uid='" + uid + '\'' +
                ", imei='" + imei + '\'' +
                ", lbs=" + lbs +
                ", time=" + time +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LBSData lbsData = (LBSData) o;

        if (imei != null ? !imei.equals(lbsData.imei) : lbsData.imei != null) return false;
        if (lbs != null ? !lbs.equals(lbsData.lbs) : lbsData.lbs != null) return false;
        if (time != null ? !time.equals(lbsData.time) : lbsData.time != null) return false;
        if (uid != null ? !uid.equals(lbsData.uid) : lbsData.uid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = uid != null ? uid.hashCode() : 0;
        result = 31 * result + (imei != null ? imei.hashCode() : 0);
        result = 31 * result + (lbs != null ? lbs.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        return result;
    }
}
