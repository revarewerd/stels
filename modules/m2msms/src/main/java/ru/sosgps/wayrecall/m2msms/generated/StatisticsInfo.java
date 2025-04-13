
package ru.sosgps.wayrecall.m2msms.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for StatisticsInfo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="StatisticsInfo"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Year" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="Month" type="{http://www.w3.org/2001/XMLSchema}unsignedByte"/&gt;
 *         &lt;element name="PacketSize" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="IncludedSMS" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="ExtraSMS" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="Remainder" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="ExternalPacketSize" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="ExternalIncludedSms" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="ExternalExtraSms" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="ExternalReminder" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="GeneralPacketSize" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="GeneralIncludedSms" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="GeneralExtraSms" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="GeneralReminder" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="MTSRemainder" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="MegafonRemainder" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="BeelineRemainder" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="RostelecomRemainder" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="Tele2Remainder" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="MotivRemainder" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="MtsTransactional" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="MegafonTransactional" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="BeelineTransactional" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="Tele2Transactional" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="MtsService" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="MegafonService" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="BeelineService" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="Tele2Service" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="TotalBilledSms" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StatisticsInfo", propOrder = {
    "year",
    "month",
    "packetSize",
    "includedSMS",
    "extraSMS",
    "remainder",
    "externalPacketSize",
    "externalIncludedSms",
    "externalExtraSms",
    "externalReminder",
    "generalPacketSize",
    "generalIncludedSms",
    "generalExtraSms",
    "generalReminder",
    "mtsRemainder",
    "megafonRemainder",
    "beelineRemainder",
    "rostelecomRemainder",
    "tele2Remainder",
    "motivRemainder",
    "mtsTransactional",
    "megafonTransactional",
    "beelineTransactional",
    "tele2Transactional",
    "mtsService",
    "megafonService",
    "beelineService",
    "tele2Service",
    "totalBilledSms"
})
public class StatisticsInfo {

    @XmlElement(name = "Year")
    protected int year;
    @XmlElement(name = "Month")
    @XmlSchemaType(name = "unsignedByte")
    protected short month;
    @XmlElement(name = "PacketSize")
    protected int packetSize;
    @XmlElement(name = "IncludedSMS")
    protected int includedSMS;
    @XmlElement(name = "ExtraSMS")
    protected int extraSMS;
    @XmlElement(name = "Remainder")
    protected int remainder;
    @XmlElement(name = "ExternalPacketSize")
    protected int externalPacketSize;
    @XmlElement(name = "ExternalIncludedSms")
    protected int externalIncludedSms;
    @XmlElement(name = "ExternalExtraSms")
    protected int externalExtraSms;
    @XmlElement(name = "ExternalReminder")
    protected int externalReminder;
    @XmlElement(name = "GeneralPacketSize")
    protected int generalPacketSize;
    @XmlElement(name = "GeneralIncludedSms")
    protected int generalIncludedSms;
    @XmlElement(name = "GeneralExtraSms")
    protected int generalExtraSms;
    @XmlElement(name = "GeneralReminder")
    protected int generalReminder;
    @XmlElement(name = "MTSRemainder")
    protected int mtsRemainder;
    @XmlElement(name = "MegafonRemainder")
    protected int megafonRemainder;
    @XmlElement(name = "BeelineRemainder")
    protected int beelineRemainder;
    @XmlElement(name = "RostelecomRemainder")
    protected int rostelecomRemainder;
    @XmlElement(name = "Tele2Remainder")
    protected int tele2Remainder;
    @XmlElement(name = "MotivRemainder")
    protected int motivRemainder;
    @XmlElement(name = "MtsTransactional")
    protected int mtsTransactional;
    @XmlElement(name = "MegafonTransactional")
    protected int megafonTransactional;
    @XmlElement(name = "BeelineTransactional")
    protected int beelineTransactional;
    @XmlElement(name = "Tele2Transactional")
    protected int tele2Transactional;
    @XmlElement(name = "MtsService")
    protected int mtsService;
    @XmlElement(name = "MegafonService")
    protected int megafonService;
    @XmlElement(name = "BeelineService")
    protected int beelineService;
    @XmlElement(name = "Tele2Service")
    protected int tele2Service;
    @XmlElement(name = "TotalBilledSms")
    protected int totalBilledSms;

    /**
     * Gets the value of the year property.
     * 
     */
    public int getYear() {
        return year;
    }

    /**
     * Sets the value of the year property.
     * 
     */
    public void setYear(int value) {
        this.year = value;
    }

    /**
     * Gets the value of the month property.
     * 
     */
    public short getMonth() {
        return month;
    }

    /**
     * Sets the value of the month property.
     * 
     */
    public void setMonth(short value) {
        this.month = value;
    }

    /**
     * Gets the value of the packetSize property.
     * 
     */
    public int getPacketSize() {
        return packetSize;
    }

    /**
     * Sets the value of the packetSize property.
     * 
     */
    public void setPacketSize(int value) {
        this.packetSize = value;
    }

    /**
     * Gets the value of the includedSMS property.
     * 
     */
    public int getIncludedSMS() {
        return includedSMS;
    }

    /**
     * Sets the value of the includedSMS property.
     * 
     */
    public void setIncludedSMS(int value) {
        this.includedSMS = value;
    }

    /**
     * Gets the value of the extraSMS property.
     * 
     */
    public int getExtraSMS() {
        return extraSMS;
    }

    /**
     * Sets the value of the extraSMS property.
     * 
     */
    public void setExtraSMS(int value) {
        this.extraSMS = value;
    }

    /**
     * Gets the value of the remainder property.
     * 
     */
    public int getRemainder() {
        return remainder;
    }

    /**
     * Sets the value of the remainder property.
     * 
     */
    public void setRemainder(int value) {
        this.remainder = value;
    }

    /**
     * Gets the value of the externalPacketSize property.
     * 
     */
    public int getExternalPacketSize() {
        return externalPacketSize;
    }

    /**
     * Sets the value of the externalPacketSize property.
     * 
     */
    public void setExternalPacketSize(int value) {
        this.externalPacketSize = value;
    }

    /**
     * Gets the value of the externalIncludedSms property.
     * 
     */
    public int getExternalIncludedSms() {
        return externalIncludedSms;
    }

    /**
     * Sets the value of the externalIncludedSms property.
     * 
     */
    public void setExternalIncludedSms(int value) {
        this.externalIncludedSms = value;
    }

    /**
     * Gets the value of the externalExtraSms property.
     * 
     */
    public int getExternalExtraSms() {
        return externalExtraSms;
    }

    /**
     * Sets the value of the externalExtraSms property.
     * 
     */
    public void setExternalExtraSms(int value) {
        this.externalExtraSms = value;
    }

    /**
     * Gets the value of the externalReminder property.
     * 
     */
    public int getExternalReminder() {
        return externalReminder;
    }

    /**
     * Sets the value of the externalReminder property.
     * 
     */
    public void setExternalReminder(int value) {
        this.externalReminder = value;
    }

    /**
     * Gets the value of the generalPacketSize property.
     * 
     */
    public int getGeneralPacketSize() {
        return generalPacketSize;
    }

    /**
     * Sets the value of the generalPacketSize property.
     * 
     */
    public void setGeneralPacketSize(int value) {
        this.generalPacketSize = value;
    }

    /**
     * Gets the value of the generalIncludedSms property.
     * 
     */
    public int getGeneralIncludedSms() {
        return generalIncludedSms;
    }

    /**
     * Sets the value of the generalIncludedSms property.
     * 
     */
    public void setGeneralIncludedSms(int value) {
        this.generalIncludedSms = value;
    }

    /**
     * Gets the value of the generalExtraSms property.
     * 
     */
    public int getGeneralExtraSms() {
        return generalExtraSms;
    }

    /**
     * Sets the value of the generalExtraSms property.
     * 
     */
    public void setGeneralExtraSms(int value) {
        this.generalExtraSms = value;
    }

    /**
     * Gets the value of the generalReminder property.
     * 
     */
    public int getGeneralReminder() {
        return generalReminder;
    }

    /**
     * Sets the value of the generalReminder property.
     * 
     */
    public void setGeneralReminder(int value) {
        this.generalReminder = value;
    }

    /**
     * Gets the value of the mtsRemainder property.
     * 
     */
    public int getMTSRemainder() {
        return mtsRemainder;
    }

    /**
     * Sets the value of the mtsRemainder property.
     * 
     */
    public void setMTSRemainder(int value) {
        this.mtsRemainder = value;
    }

    /**
     * Gets the value of the megafonRemainder property.
     * 
     */
    public int getMegafonRemainder() {
        return megafonRemainder;
    }

    /**
     * Sets the value of the megafonRemainder property.
     * 
     */
    public void setMegafonRemainder(int value) {
        this.megafonRemainder = value;
    }

    /**
     * Gets the value of the beelineRemainder property.
     * 
     */
    public int getBeelineRemainder() {
        return beelineRemainder;
    }

    /**
     * Sets the value of the beelineRemainder property.
     * 
     */
    public void setBeelineRemainder(int value) {
        this.beelineRemainder = value;
    }

    /**
     * Gets the value of the rostelecomRemainder property.
     * 
     */
    public int getRostelecomRemainder() {
        return rostelecomRemainder;
    }

    /**
     * Sets the value of the rostelecomRemainder property.
     * 
     */
    public void setRostelecomRemainder(int value) {
        this.rostelecomRemainder = value;
    }

    /**
     * Gets the value of the tele2Remainder property.
     * 
     */
    public int getTele2Remainder() {
        return tele2Remainder;
    }

    /**
     * Sets the value of the tele2Remainder property.
     * 
     */
    public void setTele2Remainder(int value) {
        this.tele2Remainder = value;
    }

    /**
     * Gets the value of the motivRemainder property.
     * 
     */
    public int getMotivRemainder() {
        return motivRemainder;
    }

    /**
     * Sets the value of the motivRemainder property.
     * 
     */
    public void setMotivRemainder(int value) {
        this.motivRemainder = value;
    }

    /**
     * Gets the value of the mtsTransactional property.
     * 
     */
    public int getMtsTransactional() {
        return mtsTransactional;
    }

    /**
     * Sets the value of the mtsTransactional property.
     * 
     */
    public void setMtsTransactional(int value) {
        this.mtsTransactional = value;
    }

    /**
     * Gets the value of the megafonTransactional property.
     * 
     */
    public int getMegafonTransactional() {
        return megafonTransactional;
    }

    /**
     * Sets the value of the megafonTransactional property.
     * 
     */
    public void setMegafonTransactional(int value) {
        this.megafonTransactional = value;
    }

    /**
     * Gets the value of the beelineTransactional property.
     * 
     */
    public int getBeelineTransactional() {
        return beelineTransactional;
    }

    /**
     * Sets the value of the beelineTransactional property.
     * 
     */
    public void setBeelineTransactional(int value) {
        this.beelineTransactional = value;
    }

    /**
     * Gets the value of the tele2Transactional property.
     * 
     */
    public int getTele2Transactional() {
        return tele2Transactional;
    }

    /**
     * Sets the value of the tele2Transactional property.
     * 
     */
    public void setTele2Transactional(int value) {
        this.tele2Transactional = value;
    }

    /**
     * Gets the value of the mtsService property.
     * 
     */
    public int getMtsService() {
        return mtsService;
    }

    /**
     * Sets the value of the mtsService property.
     * 
     */
    public void setMtsService(int value) {
        this.mtsService = value;
    }

    /**
     * Gets the value of the megafonService property.
     * 
     */
    public int getMegafonService() {
        return megafonService;
    }

    /**
     * Sets the value of the megafonService property.
     * 
     */
    public void setMegafonService(int value) {
        this.megafonService = value;
    }

    /**
     * Gets the value of the beelineService property.
     * 
     */
    public int getBeelineService() {
        return beelineService;
    }

    /**
     * Sets the value of the beelineService property.
     * 
     */
    public void setBeelineService(int value) {
        this.beelineService = value;
    }

    /**
     * Gets the value of the tele2Service property.
     * 
     */
    public int getTele2Service() {
        return tele2Service;
    }

    /**
     * Sets the value of the tele2Service property.
     * 
     */
    public void setTele2Service(int value) {
        this.tele2Service = value;
    }

    /**
     * Gets the value of the totalBilledSms property.
     * 
     */
    public int getTotalBilledSms() {
        return totalBilledSms;
    }

    /**
     * Sets the value of the totalBilledSms property.
     * 
     */
    public void setTotalBilledSms(int value) {
        this.totalBilledSms = value;
    }

}
