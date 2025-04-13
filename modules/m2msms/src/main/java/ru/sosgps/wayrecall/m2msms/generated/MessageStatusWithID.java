
package ru.sosgps.wayrecall.m2msms.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MessageStatusWithID complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MessageStatusWithID"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="MessageID" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
 *         &lt;element name="Delivery" type="{http://mcommunicator.ru/M2M}ArrayOfDeliveryInfo" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MessageStatusWithID", propOrder = {
    "messageID",
    "delivery"
})
public class MessageStatusWithID {

    @XmlElement(name = "MessageID")
    protected long messageID;
    @XmlElement(name = "Delivery")
    protected ArrayOfDeliveryInfo delivery;

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

    /**
     * Gets the value of the delivery property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfDeliveryInfo }
     *     
     */
    public ArrayOfDeliveryInfo getDelivery() {
        return delivery;
    }

    /**
     * Sets the value of the delivery property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfDeliveryInfo }
     *     
     */
    public void setDelivery(ArrayOfDeliveryInfo value) {
        this.delivery = value;
    }

}
