
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
 *         &lt;element name="GetMessagesStatusResult" type="{http://mcommunicator.ru/M2M}ArrayOfMessageStatusWithID" minOccurs="0"/&gt;
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
    "getMessagesStatusResult"
})
@XmlRootElement(name = "GetMessagesStatusResponse")
public class GetMessagesStatusResponse {

    @XmlElement(name = "GetMessagesStatusResult")
    protected ArrayOfMessageStatusWithID getMessagesStatusResult;

    /**
     * Gets the value of the getMessagesStatusResult property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfMessageStatusWithID }
     *     
     */
    public ArrayOfMessageStatusWithID getGetMessagesStatusResult() {
        return getMessagesStatusResult;
    }

    /**
     * Sets the value of the getMessagesStatusResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfMessageStatusWithID }
     *     
     */
    public void setGetMessagesStatusResult(ArrayOfMessageStatusWithID value) {
        this.getMessagesStatusResult = value;
    }

}
