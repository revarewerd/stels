
package ru.sosgps.wayrecall.m2msms.generated;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DeliveryStatus.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="DeliveryStatus"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Pending"/&gt;
 *     &lt;enumeration value="Sending"/&gt;
 *     &lt;enumeration value="Sent"/&gt;
 *     &lt;enumeration value="NotSent"/&gt;
 *     &lt;enumeration value="Delivered"/&gt;
 *     &lt;enumeration value="NotDelivered"/&gt;
 *     &lt;enumeration value="TimedOut"/&gt;
 *     &lt;enumeration value="Error"/&gt;
 *     &lt;enumeration value="Cancelled"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "DeliveryStatus")
@XmlEnum
public enum DeliveryStatus {

    @XmlEnumValue("Pending")
    PENDING("Pending"),
    @XmlEnumValue("Sending")
    SENDING("Sending"),
    @XmlEnumValue("Sent")
    SENT("Sent"),
    @XmlEnumValue("NotSent")
    NOT_SENT("NotSent"),
    @XmlEnumValue("Delivered")
    DELIVERED("Delivered"),
    @XmlEnumValue("NotDelivered")
    NOT_DELIVERED("NotDelivered"),
    @XmlEnumValue("TimedOut")
    TIMED_OUT("TimedOut"),
    @XmlEnumValue("Error")
    ERROR("Error"),
    @XmlEnumValue("Cancelled")
    CANCELLED("Cancelled");
    private final String value;

    DeliveryStatus(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DeliveryStatus fromValue(String v) {
        for (DeliveryStatus c: DeliveryStatus.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
