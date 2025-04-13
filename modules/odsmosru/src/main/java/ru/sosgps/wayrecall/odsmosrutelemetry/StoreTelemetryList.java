
package ru.sosgps.wayrecall.odsmosrutelemetry;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for storeTelemetryList complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="storeTelemetryList">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="telemetryWithDetails" type="{http://webservice.telemetry.udo.fors.ru/}telemetryWithDetails" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "storeTelemetryList", propOrder = {
    "telemetryWithDetails"
})
public class StoreTelemetryList {

    protected List<TelemetryWithDetails> telemetryWithDetails;

    /**
     * Gets the value of the telemetryWithDetails property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the telemetryWithDetails property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTelemetryWithDetails().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TelemetryWithDetails }
     * 
     * 
     */
    public List<TelemetryWithDetails> getTelemetryWithDetails() {
        if (telemetryWithDetails == null) {
            telemetryWithDetails = new ArrayList<TelemetryWithDetails>();
        }
        return this.telemetryWithDetails;
    }

}
