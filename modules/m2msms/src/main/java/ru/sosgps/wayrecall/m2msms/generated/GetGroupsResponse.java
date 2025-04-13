
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
 *         &lt;element name="GetGroupsResult" type="{http://mcommunicator.ru/M2M}ArrayOfSubscriberGroupInfo" minOccurs="0"/&gt;
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
    "getGroupsResult"
})
@XmlRootElement(name = "GetGroupsResponse")
public class GetGroupsResponse {

    @XmlElement(name = "GetGroupsResult")
    protected ArrayOfSubscriberGroupInfo getGroupsResult;

    /**
     * Gets the value of the getGroupsResult property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfSubscriberGroupInfo }
     *     
     */
    public ArrayOfSubscriberGroupInfo getGetGroupsResult() {
        return getGroupsResult;
    }

    /**
     * Sets the value of the getGroupsResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfSubscriberGroupInfo }
     *     
     */
    public void setGetGroupsResult(ArrayOfSubscriberGroupInfo value) {
        this.getGroupsResult = value;
    }

}
