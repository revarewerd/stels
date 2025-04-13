
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
 *         &lt;element name="SendMessageWithValidityPeriodResult" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
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
    "sendMessageWithValidityPeriodResult"
})
@XmlRootElement(name = "SendMessageWithValidityPeriodResponse")
public class SendMessageWithValidityPeriodResponse {

    @XmlElement(name = "SendMessageWithValidityPeriodResult")
    protected long sendMessageWithValidityPeriodResult;

    /**
     * Gets the value of the sendMessageWithValidityPeriodResult property.
     * 
     */
    public long getSendMessageWithValidityPeriodResult() {
        return sendMessageWithValidityPeriodResult;
    }

    /**
     * Sets the value of the sendMessageWithValidityPeriodResult property.
     * 
     */
    public void setSendMessageWithValidityPeriodResult(long value) {
        this.sendMessageWithValidityPeriodResult = value;
    }

}
