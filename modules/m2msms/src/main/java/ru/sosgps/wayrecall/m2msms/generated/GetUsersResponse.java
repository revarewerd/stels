
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
 *         &lt;element name="GetUsersResult" type="{http://mcommunicator.ru/M2M}ArrayOfUserInfo" minOccurs="0"/&gt;
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
    "getUsersResult"
})
@XmlRootElement(name = "GetUsersResponse")
public class GetUsersResponse {

    @XmlElement(name = "GetUsersResult")
    protected ArrayOfUserInfo getUsersResult;

    /**
     * Gets the value of the getUsersResult property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfUserInfo }
     *     
     */
    public ArrayOfUserInfo getGetUsersResult() {
        return getUsersResult;
    }

    /**
     * Sets the value of the getUsersResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfUserInfo }
     *     
     */
    public void setGetUsersResult(ArrayOfUserInfo value) {
        this.getUsersResult = value;
    }

}
