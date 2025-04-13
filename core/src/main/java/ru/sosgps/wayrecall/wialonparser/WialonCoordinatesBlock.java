/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.sosgps.wayrecall.wialonparser;

/**
 * @author nickl
 */
public class WialonCoordinatesBlock extends WialonPackageBlock {

    public double lon;
    public double lat;
    public double height;
    public short speed;
    public short course;
    public byte satelliteNum;

    @Override
    public String toString() {
        return super.toString() + "\n lon:" + lon + " lat:" + lat + " height:" + height + " speed:" + speed + " course:" + course + " snum:" + satelliteNum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        WialonCoordinatesBlock that = (WialonCoordinatesBlock) o;

        if (course != that.course) return false;
        if (Double.compare(that.height, height) != 0) return false;
        if (Double.compare(that.lat, lat) != 0) return false;
        if (Double.compare(that.lon, lon) != 0) return false;
        if (satelliteNum != that.satelliteNum) return false;
        if (speed != that.speed) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(lon);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lat);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(height);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (int) speed;
        result = 31 * result + (int) course;
        result = 31 * result + (int) satelliteNum;
        return result;
    }
}
