
package ru.sosgps.wayrecall.m2msms.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="SendVariousMessagesResult" type="{http://mcommunicator.ru/M2M}ArrayOfSendMessageIDs" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "sendVariousMessagesResult"
})
@XmlRootElement(name = "SendVariousMessagesResponse")
public class SendVariousMessagesResponse {

    @XmlElement(name = "SendVariousMessagesResult")
    protected ArrayOfSendMessageIDs sendVariousMessagesResult;

    /**
     * Gets the value of the sendVariousMessagesResult property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfSendMessageIDs }
     *     
     */
    public ArrayOfSendMessageIDs getSendVariousMessagesResult() {
        return sendVariousMessagesResult;
    }

    /**
     * Sets the value of the sendVariousMessagesResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfSendMessageIDs }
     *     
     */
    public void setSendVariousMessagesResult(ArrayOfSendMessageIDs value) {
        this.sendVariousMessagesResult = value;
    }

}
