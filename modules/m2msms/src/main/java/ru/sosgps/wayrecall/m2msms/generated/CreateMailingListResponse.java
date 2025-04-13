
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
 *         &lt;element name="CreateMailingListResult" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
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
    "createMailingListResult"
})
@XmlRootElement(name = "CreateMailingListResponse")
public class CreateMailingListResponse {

    @XmlElement(name = "CreateMailingListResult")
    protected long createMailingListResult;

    /**
     * Gets the value of the createMailingListResult property.
     * 
     */
    public long getCreateMailingListResult() {
        return createMailingListResult;
    }

    /**
     * Sets the value of the createMailingListResult property.
     * 
     */
    public void setCreateMailingListResult(long value) {
        this.createMailingListResult = value;
    }

}
