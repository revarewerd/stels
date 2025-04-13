
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
 *         &lt;element name="GetStatisticsResult" type="{http://mcommunicator.ru/M2M}StatisticsInfo" minOccurs="0"/&gt;
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
    "getStatisticsResult"
})
@XmlRootElement(name = "GetStatisticsResponse")
public class GetStatisticsResponse {

    @XmlElement(name = "GetStatisticsResult")
    protected StatisticsInfo getStatisticsResult;

    /**
     * Gets the value of the getStatisticsResult property.
     * 
     * @return
     *     possible object is
     *     {@link StatisticsInfo }
     *     
     */
    public StatisticsInfo getGetStatisticsResult() {
        return getStatisticsResult;
    }

    /**
     * Sets the value of the getStatisticsResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link StatisticsInfo }
     *     
     */
    public void setGetStatisticsResult(StatisticsInfo value) {
        this.getStatisticsResult = value;
    }

}
