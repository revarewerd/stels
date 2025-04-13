
package ru.sosgps.wayrecall.m2msms.generated;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for RequestMessageType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="RequestMessageType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="All"/&gt;
 *     &lt;enumeration value="MO"/&gt;
 *     &lt;enumeration value="MT"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "RequestMessageType")
@XmlEnum
public enum RequestMessageType {

    @XmlEnumValue("All")
    ALL("All"),
    MO("MO"),
    MT("MT");
    private final String value;

    RequestMessageType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static RequestMessageType fromValue(String v) {
        for (RequestMessageType c: RequestMessageType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
