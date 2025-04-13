
package ru.sosgps.wayrecall.odsmosrutelemetry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for telemetryBa complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="telemetryBa">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="coordX" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="coordY" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="date" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="glonass" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="gpsCode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="speed" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "telemetryBa", propOrder = {
    "coordX",
    "coordY",
    "date",
    "glonass",
    "gpsCode",
    "speed"
})
public class TelemetryBa {

    protected double coordX;
    protected double coordY;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar date;
    protected boolean glonass;
    @XmlElement(required = true)
    protected String gpsCode;
    protected double speed;

    /**
     * Gets the value of the coordX property.
     * 
     */
    public double getCoordX() {
        return coordX;
    }

    /**
     * Sets the value of the coordX property.
     * 
     */
    public void setCoordX(double value) {
        this.coordX = value;
    }

    /**
     * Gets the value of the coordY property.
     * 
     */
    public double getCoordY() {
        return coordY;
    }

    /**
     * Sets the value of the coordY property.
     * 
     */
    public void setCoordY(double value) {
        this.coordY = value;
    }

    /**
     * Gets the value of the date property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getDate() {
        return date;
    }

    /**
     * Sets the value of the date property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setDate(XMLGregorianCalendar value) {
        this.date = value;
    }

    /**
     * Gets the value of the glonass property.
     * 
     */
    public boolean isGlonass() {
        return glonass;
    }

    /**
     * Sets the value of the glonass property.
     * 
     */
    public void setGlonass(boolean value) {
        this.glonass = value;
    }

    /**
     * Gets the value of the gpsCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGpsCode() {
        return gpsCode;
    }

    /**
     * Sets the value of the gpsCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGpsCode(String value) {
        this.gpsCode = value;
    }

    /**
     * Gets the value of the speed property.
     * 
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Sets the value of the speed property.
     * 
     */
    public void setSpeed(double value) {
        this.speed = value;
    }

}
