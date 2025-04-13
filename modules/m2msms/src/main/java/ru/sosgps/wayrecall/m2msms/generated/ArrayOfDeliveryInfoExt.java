
package ru.sosgps.wayrecall.m2msms.generated;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfDeliveryInfoExt complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfDeliveryInfoExt"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="DeliveryInfoExt" type="{http://mcommunicator.ru/M2M}DeliveryInfoExt" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfDeliveryInfoExt", propOrder = {
    "deliveryInfoExt"
})
public class ArrayOfDeliveryInfoExt {

    @XmlElement(name = "DeliveryInfoExt", nillable = true)
    protected List<DeliveryInfoExt> deliveryInfoExt;

    /**
     * Gets the value of the deliveryInfoExt property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the deliveryInfoExt property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDeliveryInfoExt().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DeliveryInfoExt }
     * 
     * 
     */
    public List<DeliveryInfoExt> getDeliveryInfoExt() {
        if (deliveryInfoExt == null) {
            deliveryInfoExt = new ArrayList<DeliveryInfoExt>();
        }
        return this.deliveryInfoExt;
    }

}
