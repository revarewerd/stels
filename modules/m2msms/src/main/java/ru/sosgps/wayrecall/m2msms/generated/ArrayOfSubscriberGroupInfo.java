
package ru.sosgps.wayrecall.m2msms.generated;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfSubscriberGroupInfo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfSubscriberGroupInfo"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="SubscriberGroupInfo" type="{http://mcommunicator.ru/M2M}SubscriberGroupInfo" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfSubscriberGroupInfo", propOrder = {
    "subscriberGroupInfo"
})
public class ArrayOfSubscriberGroupInfo {

    @XmlElement(name = "SubscriberGroupInfo", nillable = true)
    protected List<SubscriberGroupInfo> subscriberGroupInfo;

    /**
     * Gets the value of the subscriberGroupInfo property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the subscriberGroupInfo property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSubscriberGroupInfo().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SubscriberGroupInfo }
     * 
     * 
     */
    public List<SubscriberGroupInfo> getSubscriberGroupInfo() {
        if (subscriberGroupInfo == null) {
            subscriberGroupInfo = new ArrayList<SubscriberGroupInfo>();
        }
        return this.subscriberGroupInfo;
    }

}
