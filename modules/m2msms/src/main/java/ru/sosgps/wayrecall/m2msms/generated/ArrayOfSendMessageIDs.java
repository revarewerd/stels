
package ru.sosgps.wayrecall.m2msms.generated;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfSendMessageIDs complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfSendMessageIDs"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="SendMessageIDs" type="{http://mcommunicator.ru/M2M}SendMessageIDs" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfSendMessageIDs", propOrder = {
    "sendMessageIDs"
})
public class ArrayOfSendMessageIDs {

    @XmlElement(name = "SendMessageIDs", nillable = true)
    protected List<SendMessageIDs> sendMessageIDs;

    /**
     * Gets the value of the sendMessageIDs property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sendMessageIDs property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSendMessageIDs().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SendMessageIDs }
     * 
     * 
     */
    public List<SendMessageIDs> getSendMessageIDs() {
        if (sendMessageIDs == null) {
            sendMessageIDs = new ArrayList<SendMessageIDs>();
        }
        return this.sendMessageIDs;
    }

}
