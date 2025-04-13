
package ru.sosgps.wayrecall.m2msms.generated;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfMailingListContact complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfMailingListContact"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="MailingListContact" type="{http://mcommunicator.ru/M2M}MailingListContact" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfMailingListContact", propOrder = {
    "mailingListContact"
})
public class ArrayOfMailingListContact {

    @XmlElement(name = "MailingListContact", nillable = true)
    protected List<MailingListContact> mailingListContact;

    /**
     * Gets the value of the mailingListContact property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the mailingListContact property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMailingListContact().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MailingListContact }
     * 
     * 
     */
    public List<MailingListContact> getMailingListContact() {
        if (mailingListContact == null) {
            mailingListContact = new ArrayList<MailingListContact>();
        }
        return this.mailingListContact;
    }

}
