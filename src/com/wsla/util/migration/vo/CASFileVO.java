package com.wsla.util.migration.vo;

import java.util.Date;

import com.siliconmtn.annotations.Importable;
import com.siliconmtn.util.StringUtil;

import com.wsla.data.provider.ProviderLocationVO.Status;

/****************************************************************************
 * <p><b>Title:</b> CASFileVO.java</p>
 * <p><b>Description:</b> models DM-CAS.xlsx provided by Steve</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 10, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class CASFileVO {

	private String casId;
	private String casName;
	private String address;
	private String address2;
	private String city;
	private String state;
	private String zip;
	private String contactName; //vendor contact
	private String contactPhone;
	private String contactEmail;
	private String status;
	private String certifications;
	private int active;
	private Date updateDate;


	public String getCasId() {
		return casId;
	}
	public String getCasName() {
		return casName;
	}
	public String getAddress() {
		return address;
	}
	public String getAddress2() {
		return address2;
	}
	public String getCity() {
		return city;
	}
	public String getState() {
		return state;
	}
	public String getZip() {
		return zip;
	}
	public String getContactName() {
		return contactName;
	}
	public String getContactPhone() {
		return contactPhone;
	}
	public String getContactEmail() {
		return contactEmail;
	}
	public String getStatus() {
		return status;
	}

	public Status getStatusEnum() {
		switch (StringUtil.checkVal(status).toUpperCase().trim()) {
			default:
			case "1. WS-PENDCONTINI": return Status.PENDING_CONTACT; 
			case "2. WS-ENVEMLINI": return Status.EMAIL_SENT;
			case "3. WS-RECIBQUES": return Status.RCVD_QUESTIONNAIRE;
			case "4. WS -REVISQUES": return Status.REVW_QUESTIONNAIRE;
			case "5. WS-NOAUTOCAS": return Status.REJECTED;
			case "6. WS-SI AUTOCAS": return Status.AUTHORIZED;
			case "7. WS-ENVCONTRAT": return Status.SEND_CONTRACT;
			case "8. WS-PENDCONTRAT": return Status.AWAITING_CONTRACT;
			case "9. WS-CONTRATFIRM": return Status.SIGNED_CONTRACT;
		}
	}

	public String getCertifications() {
		return certifications;
	}
	public int getActive() {
		return active;
	}
	public Date getUpdateDate() {
		return updateDate;
	}


	@Importable(name="Tech Number")
	public void setCasId(String casId) {
		this.casId = casId;
	}
	@Importable(name="Tech Name")
	public void setCasName(String casName) {
		this.casName = casName;
	}
	@Importable(name="Address 1")
	public void setAddress(String address) {
		this.address = address;
	}
	@Importable(name="Address 2")
	public void setAddress2(String address2) {
		this.address2 = address2;
	}
	@Importable(name="City, STATE")
	public void setCity(String city) {
		if (StringUtil.isEmpty(city)) return;
		this.city = city;

		if (city.indexOf(',') > -1) {
			String[] arr = city.split(",");
			this.city = arr[0].trim();
			setState(arr[1].trim());
		}
	}
	public void setState(String state) {
		this.state = state;
	}
	@Importable(name="Svc Zip Codes")
	public void setZip(String zip) {
		this.zip = zip;
	}
	@Importable(name="Vendor Contact")
	public void setContactName(String contactName) {
		this.contactName = contactName;
	}
	@Importable(name="Tech Phone")
	public void setContactPhone(String contactPhone) {
		this.contactPhone = contactPhone;
	}
	@Importable(name="E-Mail Address")
	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}
	@Importable(name="Status Actual")
	public void setStatus(String status) {
		this.status = status;
	}
	@Importable(name="Certificacion")
	public void addCertification(String addtlCert) {
		if (StringUtil.isEmpty(addtlCert)) return;
		if (!StringUtil.isEmpty(certifications)) certifications += ", ";
		this.certifications += addtlCert;
	}
	@Importable(name="Active?")
	public void setActive(String active) {
		this.active = "Y".equalsIgnoreCase(active) ? 1 : 0;
	}

	@Importable(name="Fecha Actualiza")
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}
}