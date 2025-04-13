
package ru.sosgps.wayrecall.m2msms.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
 *         &lt;element name="msidsAndMessages" type="{http://mcommunicator.ru/M2M}ArrayOfSubmit" minOccurs="0"/&gt;
 *         &lt;element name="naming" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="scheduledSendDate" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="validityPeriod" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="login" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="password" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
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
    "msidsAndMessages",
    "naming",
    "scheduledSendDate",
    "validityPeriod",
    "login",
    "password"
})
@XmlRootElement(name = "SendVariousMessages")
public class SendVariousMessages {

    protected ArrayOfSubmit msidsAndMessages;
    protected String naming;
    protected String scheduledSendDate;
    protected String validityPeriod;
    protected String login;
    protected String password;

    /**
     * Gets the value of the msidsAndMessages property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfSubmit }
     *     
     */
    public ArrayOfSubmit getMsidsAndMessages() {
        return msidsAndMessages;
    }

    /**
     * Sets the value of the msidsAndMessages property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfSubmit }
     *     
     */
    public void setMsidsAndMessages(ArrayOfSubmit value) {
        this.msidsAndMessages = value;
    }

    /**
     * Gets the value of the naming property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNaming() {
        return naming;
    }

    /**
     * Sets the value of the naming property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNaming(String value) {
        this.naming = value;
    }

    /**
     * Gets the value of the scheduledSendDate property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getScheduledSendDate() {
        return scheduledSendDate;
    }

    /**
     * Sets the value of the scheduledSendDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setScheduledSendDate(String value) {
        this.scheduledSendDate = value;
    }

    /**
     * Gets the value of the validityPeriod property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValidityPeriod() {
        return validityPeriod;
    }

    /**
     * Sets the value of the validityPeriod property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValidityPeriod(String value) {
        this.validityPeriod = value;
    }

    /**
     * Gets the value of the login property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLogin() {
        return login;
    }

    /**
     * Sets the value of the login property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLogin(String value) {
        this.login = value;
    }

    /**
     * Gets the value of the password property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the value of the password property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPassword(String value) {
        this.password = value;
    }

}
