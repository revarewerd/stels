/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.sosgps.wayrecall.core;

import scala.SerialVersionUID;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author nickl
 */
public class GPSData implements Serializable, Cloneable {

    private static final long serialVersionUID = -6497944705467710L;
    /**
     * Идентификатор объекта, к которому данное устройство приписано
     */
    public String uid;
    /**
     * Идетификатор устройства, который был прислан вместе с пакетом.
     */
    public String imei;
    public double lon = Double.NaN;
    public double lat = Double.NaN;
    public Date time;
    public short speed = -1;
    public short course = -1;
    public byte satelliteNum = -1;
    public boolean goodlonlat = true;
    public String placeName;
    public Date insertTime;

    public Map<String, Object> data = new HashMap<String, Object>();
    public Map<String, Object> privateData = new HashMap<String, Object>();

    public GPSData(String uid, String imei, double lon, double lat, Date time, short speed, short course, byte satelliteNum, String placeName, Date insertTime) {
        this(uid, imei, lon, lat, time, speed, course, satelliteNum);
        this.placeName = placeName;
        this.insertTime = insertTime;
    }




    public GPSData(String uid, String imei, double lon, double lat, Date time, short speed, short course, byte satelliteNum, String placeName, Date insertTime, Map<String, Object> data) {
        this(uid, imei, lon, lat, time, speed, course, satelliteNum, placeName, insertTime);
        this.data = data;
    }

    public GPSData(String uid, String imei, double lon, double lat, Date time, short speed, short course, byte satelliteNum) {
        this(uid, imei, time);
        this.speed = speed;
        this.course = course;
        this.lon = lon;
        this.lat = lat;
        this.satelliteNum = satelliteNum;
    }

    public GPSData(String uid, String imei, Date time) {
        this.imei = imei;
        this.uid = uid;
        this.time = time;
    }

    @Override
    public String toString() {
        try {
            return "GPSData{" +
                    "uid='" + uid + '\'' +
                    ", imei='" + imei + '\'' +
                    ", lon=" + lon +
                    ", lat=" + lat +
                    ", time=" + time +
                    ", speed=" + speed +
                    ", course=" + course +
                    ", satelliteNum=" + satelliteNum +
                    ", goodlonlat=" + goodlonlat +
                    ", placeName='" + placeName + '\'' +
                    ", insertTime=" + insertTime +
                    ", data=" + new TreeMap<>(data) +
                    ", privateData=" + new TreeMap<>(privateData) +
                    '}';
        } catch (java.util.ConcurrentModificationException e) {
            return "GPSData{" +
                    "uid='" + uid + '\'' +
                    ", imei='" + imei + '\'' +
                    ", lon=" + lon +
                    ", lat=" + lat +
                    ", time=" + time +
                    ", speed=" + speed +
                    ", course=" + course +
                    ", satelliteNum=" + satelliteNum +
                    ", goodlonlat=" + goodlonlat +
                    ", placeName='" + placeName + '\'' +
                    ", insertTime=" + insertTime +
                    ", data= {not avaliable}" +
                    '}';
        }
    }

    @Override
    public boolean equals(Object o) {
        return equalsIngoringData(o, false);
    }

    public boolean equalsIngoringData(Object o, boolean ingoreData) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GPSData gpsData = (GPSData) o;

        if (course != gpsData.course) return false;
        if (goodlonlat != gpsData.goodlonlat) return false;
        if (Double.compare(gpsData.lat, lat) != 0) return false;
        if (Double.compare(gpsData.lon, lon) != 0) return false;
        if (satelliteNum != gpsData.satelliteNum) return false;
        if (speed != gpsData.speed) return false;
        if (!ingoreData && (data != null ? !data.equals(gpsData.data) : gpsData.data != null)) return false;
        if (imei != null ? !imei.equals(gpsData.imei) : gpsData.imei != null) return false;
        if (insertTime != null ? !insertTime.equals(gpsData.insertTime) : gpsData.insertTime != null) return false;
        if (placeName != null ? !placeName.equals(gpsData.placeName) : gpsData.placeName != null) return false;
        if (time != null ? !time.equals(gpsData.time) : gpsData.time != null) return false;
        if (uid != null ? !uid.equals(gpsData.uid) : gpsData.uid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = uid != null ? uid.hashCode() : 0;
        result = 31 * result + (imei != null ? imei.hashCode() : 0);
        temp = Double.doubleToLongBits(lon);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lat);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (time != null ? time.hashCode() : 0);
        result = 31 * result + (int) speed;
        result = 31 * result + (int) course;
        result = 31 * result + (int) satelliteNum;
        result = 31 * result + (goodlonlat ? 1 : 0);
        result = 31 * result + (placeName != null ? placeName.hashCode() : 0);
        result = 31 * result + (insertTime != null ? insertTime.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    public boolean containsLonLat() {
        return !Double.isNaN(lon) && !Double.isNaN(lat) && !(lon == 0.0) && !(lat == 0.0);
    }

    public static double manhdistance(GPSData a, GPSData b) {
        return Math.abs(a.lat - b.lat) + Math.abs(a.lon - b.lon);
    }

    public static double manhdistance(GPSData a, double lon, double lat) {
        return Math.abs(a.lat - lat) + Math.abs(a.lon - lon);
    }

    public static double eulcidian2distance(GPSData a, double lon, double lat) {
        double latd = (a.lat - lat);
        double lond = (a.lon - lon);
        return (latd * latd) + (lond * lond);
    }

    @Override
    public GPSData clone() {
        final GPSData clone;
        try {
            clone = (GPSData) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        clone.data = new HashMap<>(data);
        clone.privateData = new HashMap<>(privateData);
        return clone;
    }

    @Deprecated
    /**
     * @deprecated No unchained messages anymore, now all messages are unordered
     */
    public boolean unchained() {
        return java.lang.Boolean.TRUE.equals(data.get("unchained"));
    }
}
