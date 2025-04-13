
package ru.sosgps.wayrecall.m2msms.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for DeliveryInfo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DeliveryInfo"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Msid" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="DeliveryStatus" type="{http://mcommunicator.ru/M2M}DeliveryStatus"/&gt;
 *         &lt;element name="DeliveryDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="UserDeliveryDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="PartCount" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DeliveryInfo", propOrder = {
    "msid",
    "deliveryStatus",
    "deliveryDate",
    "userDeliveryDate",
    "partCount"
})
public class DeliveryInfo {

    @XmlElement(name = "Msid")
    protected String msid;
    @XmlElement(name = "DeliveryStatus", required = true)
    @XmlSchemaType(name = "string")
    protected DeliveryStatus deliveryStatus;
    @XmlElement(name = "DeliveryDate", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar deliveryDate;
    @XmlElement(name = "UserDeliveryDate", required = true, nillable = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar userDeliveryDate;
    @XmlElement(name = "PartCount")
    protected int partCount;

    /**
     * Gets the value of the msid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMsid() {
        return msid;
    }

    /**
     * Sets the value of the msid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMsid(String value) {
        this.msid = value;
    }

    /**
     * Gets the value of the deliveryStatus property.
     * 
     * @return
     *     possible object is
     *     {@link DeliveryStatus }
     *     
     */
    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    /**
     * Sets the value of the deliveryStatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link DeliveryStatus }
     *     
     */
    public void setDeliveryStatus(DeliveryStatus value) {
        this.deliveryStatus = value;
    }

    /**
     * Gets the value of the deliveryDate property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getDeliveryDate() {
        return deliveryDate;
    }

    /**
     * Sets the value of the deliveryDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setDeliveryDate(XMLGregorianCalendar value) {
        this.deliveryDate = value;
    }

    /**
     * Gets the value of the userDeliveryDate property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getUserDeliveryDate() {
        return userDeliveryDate;
    }

    /**
     * Sets the value of the userDeliveryDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setUserDeliveryDate(XMLGregorianCalendar value) {
        this.userDeliveryDate = value;
    }

    /**
     * Gets the value of the partCount property.
     * 
     */
    public int getPartCount() {
        return partCount;
    }

    /**
     * Sets the value of the partCount property.
     * 
     */
    public void setPartCount(int value) {
        this.partCount = value;
    }

}
