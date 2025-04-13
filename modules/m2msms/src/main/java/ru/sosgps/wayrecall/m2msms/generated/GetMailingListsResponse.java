
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
 *         &lt;element name="GetMailingListsResult" type="{http://mcommunicator.ru/M2M}ArrayOfResultMailingList" minOccurs="0"/&gt;
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
    "getMailingListsResult"
})
@XmlRootElement(name = "GetMailingListsResponse")
public class GetMailingListsResponse {

    @XmlElement(name = "GetMailingListsResult")
    protected ArrayOfResultMailingList getMailingListsResult;

    /**
     * Gets the value of the getMailingListsResult property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfResultMailingList }
     *     
     */
    public ArrayOfResultMailingList getGetMailingListsResult() {
        return getMailingListsResult;
    }

    /**
     * Sets the value of the getMailingListsResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfResultMailingList }
     *     
     */
    public void setGetMailingListsResult(ArrayOfResultMailingList value) {
        this.getMailingListsResult = value;
    }

}
