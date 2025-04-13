
package ru.sosgps.wayrecall.m2msms.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SendMessageIDs complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SendMessageIDs"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Msid" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="MessageID" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SendMessageIDs", propOrder = {
    "msid",
    "messageID"
})
public class SendMessageIDs {

    @XmlElement(name = "Msid")
    protected String msid;
    @XmlElement(name = "MessageID")
    protected long messageID;

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
     * Gets the value of the messageID property.
     * 
     */
    public long getMessageID() {
        return messageID;
    }

    /**
     * Sets the value of the messageID property.
     * 
     */
    public void setMessageID(long value) {
        this.messageID = value;
    }

}
