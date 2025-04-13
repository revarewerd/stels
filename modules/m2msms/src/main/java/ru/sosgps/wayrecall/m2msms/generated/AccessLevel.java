
package ru.sosgps.wayrecall.m2msms.generated;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for AccessLevel.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="AccessLevel"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Administrator"/&gt;
 *     &lt;enumeration value="Operator"/&gt;
 *     &lt;enumeration value="BaseUser"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "AccessLevel")
@XmlEnum
public enum AccessLevel {

    @XmlEnumValue("Administrator")
    ADMINISTRATOR("Administrator"),
    @XmlEnumValue("Operator")
    OPERATOR("Operator"),
    @XmlEnumValue("BaseUser")
    BASE_USER("BaseUser");
    private final String value;

    AccessLevel(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AccessLevel fromValue(String v) {
        for (AccessLevel c: AccessLevel.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
