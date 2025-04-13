/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.sosgps.wayrecall.wialonparser;

/**
 * @author nickl
 */
public class WialonPackageBlock {

    public short type;
    public int size;
    public boolean hidden;
    public byte dataType;
    public String name;
    public Object value;

    @Override
    public String toString() {
        return "WialonPackageBlock: name:" + name + " type:" + type + " size:" + size + " hidden:" + hidden + " dataType:" + dataType + " value:" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WialonPackageBlock that = (WialonPackageBlock) o;

        if (dataType != that.dataType) return false;
        if (hidden != that.hidden) return false;
        if (size != that.size) return false;
        if (type != that.type) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) type;
        result = 31 * result + size;
        result = 31 * result + (hidden ? 1 : 0);
        result = 31 * result + (int) dataType;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
