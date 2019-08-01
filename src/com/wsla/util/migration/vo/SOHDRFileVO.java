package com.wsla.util.migration.vo;

import java.util.Date;

import com.siliconmtn.annotations.Importable;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketOriginCode;

/****************************************************************************
 * <p><b>Title:</b> SOHeaderFileVO.java</p>
 * <p><b>Description:</b> The SOHDR Excel file from Steve.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Feb 1, 2019
 * <b>Changes:</b>
 ***********************s*****************************************************/
public class SOHDRFileVO {

	private String soNumber;
	private Date receivedDate;
	private Date closedDate;
	private String serviceTech;
	private String territory;
	private Date altKeyDate;
	private String equipmentId;
	private String manufacturer;
	private String serialNumber;
	private String soType;
	//customer fields
	private String custName;
	private String custAddress1;
	private String custAddress2;
	private String custCity;
	private String custState;
	private String custZip;
	private String custCountry;
	private String custPhone;
	private String custContact;
	private String problemCode; //reported problem
	private String coverageCode; //warrantied (covered/not-covered)
	private String custPO;

	private String operator; //call ctr rep
	private String receivedMethod; //how they contacted WSLA
	private String status;
	private Date startDate;
	private Date statusDate;
	private String userArea1;
	private String userArea2;
	private String userArea3;
	private String emailAddress;
	private String productCategory;

	public String getSoNumber() {
		return soNumber;
	}
	public Date getReceivedDate() {
		return receivedDate;
	}
	public Date getClosedDate() {
		return closedDate;
	}
	public String getServiceTech() {
		return serviceTech;
	}
	public String getTerritory() {
		return territory;
	}
	public Date getAltKeyDate() {
		return altKeyDate;
	}
	public String getEquipmentId() {
		return equipmentId;
	}
	public String getManufacturer() {
		return manufacturer;
	}
	public String getSerialNumber() {
		return serialNumber;
	}
	public String getSoType() {
		return soType;
	}
	public String getCustName() {
		return custName;
	}
	public String getCustAddress1() {
		return custAddress1;
	}
	public String getCustAddress2() {
		return custAddress2;
	}
	public String getCustState() {
		return custState;
	}
	public String getCustZip() {
		//flush all zeros
		return custZip == null || custZip.matches("0+") ? null : custZip;
	}
	public String getCustPhone() {
		custPhone = StringUtil.removeNonNumeric(custPhone);
		if (custPhone == null || custPhone.length() < 11) {
			return custPhone;
		} else {
			//return 10 digits from the right - which gets rid of weird area and country codes
			return custPhone.substring(custPhone.length()-10);
		}
	}
	public String getCustContact() {
		return custContact;
	}
	public String getProblemCode() {
		return problemCode;
	}
	public String getCoverageCode() {
		return coverageCode;
	}
	public String getCustPO() {
		return custPO;
	}
	public String getOperator() {
		return operator;
	}
	public String getReceivedMethod() {
		if (StringUtil.isEmpty(receivedMethod)) return null;
		if (receivedMethod.matches("(?i:.*email.*)")) return TicketOriginCode.EMAIL.name();
		if (receivedMethod.matches("(?i:.*sales?for.*)")) return TicketOriginCode.SALESFORCE.name();
		return TicketOriginCode.CALL.name(); //LLAMDA - the default per Steve
	}
	public String getStatus() {
		return status;
	}
	public StatusCode getStatusCode() {
		switch (StringUtil.checkVal(status).toUpperCase()) {
			case "D":
			case "B":
			case "C": return StatusCode.CLOSED;
			case "P": return StatusCode.CAS_ASSIGNED;
			case "S": return StatusCode.DELIVERY_SCHEDULED;
			case "E":
			default: return StatusCode.OPENED;
		}
	}
	public Date getStartDate() {
		return startDate;
	}
	public Date getStatusDate() {
		return statusDate;
	}
	public String getUserArea1() {
		return userArea1;
	}
	public String getUserArea2() {
		return userArea2;
	}
	public String getUserArea3() {
		return userArea3;
	}	
	public String getEmailAddress() {
		return StringUtil.isValidEmail(emailAddress) ? emailAddress : null;
	}
	public String getCustCity() {
		return custCity;
	}
	public String getProductCategory() {
		return StringUtil.checkVal(productCategory, "FPT");
	}
	public String getCustCountry() {
		return custCountry;
	}


	@Importable(name="SO Number")
	public void setSoNumber(String soNumber) {
		this.soNumber = soNumber;
	}
	@Importable(name="Date Received")
	public void setReceivedDate(Date receivedDate) {
		this.receivedDate = receivedDate;
	}
	@Importable(name="Date Closed")
	public void setClosedDate(Date closedDate) {
		this.closedDate = closedDate;
	}
	@Importable(name="Service Tech")
	public void setServiceTech(String serviceTech) {
		this.serviceTech = serviceTech;
	}
	@Importable(name="Territory")
	public void setTerritory(String territory) {
		this.territory = territory;
	}
	@Importable(name="Date Altkey (rec'd)")
	public void setAltKeyDate(Date altKeyDate) {
		this.altKeyDate = altKeyDate;
	}
	@Importable(name="Equipment ID")
	public void setEquipmentId(String equipmentId) {
		this.equipmentId = equipmentId;
	}
	@Importable(name="Manufacturer")
	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}
	@Importable(name="Serial Number")
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}
	@Importable(name="Svc Order Type")
	public void setSoType(String soType) {
		this.soType = soType;
	}
	@Importable(name="Name")
	public void setCustName(String custName) {
		this.custName = custName;
	}
	@Importable(name="Svc Address 1")
	public void setCustAddress1(String custAddress1) {
		this.custAddress1 = custAddress1;
	}
	@Importable(name="Svc Address 2")
	public void setCustAddress2(String custAddress2) {
		this.custAddress2 = custAddress2;
	}
	@Importable(name="Svc St")
	public void setCustState(String custState) {
		this.custState = custState;
	}
	@Importable(name="Svc Zip")
	public void setCustZip(String custZip) {
		this.custZip = custZip;
	}
	@Importable(name="Phone Number")
	public void setCustPhone(String custPhone) {
		this.custPhone = custPhone;
	}
	@Importable(name="Contact")
	public void setCustContact(String custContact) {
		this.custContact = custContact;
	}
	@Importable(name="Problem Code")
	public void setProblemCode(String problemCode) {
		this.problemCode = problemCode;
	}
	@Importable(name="Coverage Code")
	public void setCoverageCode(String coverageCode) {
		this.coverageCode = coverageCode;
	}
	@Importable(name="Customer P O")
	public void setCustPO(String custPO) {
		this.custPO = custPO;
	}
	@Importable(name="Operator")
	public void setOperator(String operator) {
		this.operator = operator;
	}
	@Importable(name="Received Method")
	public void setReceivedMethod(String receivedMethod) {
		this.receivedMethod = receivedMethod;
	}
	@Importable(name="Status")
	public void setStatus(String status) {
		this.status = status;
	}
	@Importable(name="Status Date")
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	@Importable(name="Date Started")
	public void setStatusDate(Date statusDate) {
		this.statusDate = statusDate;
	}
	@Importable(name="User Area 1")
	public void setUserArea1(String userArea1) {
		this.userArea1 = userArea1;
	}
	@Importable(name="User Area 2")
	public void setUserArea2(String userArea2) {
		this.userArea2 = userArea2;
	}
	@Importable(name="User Area 3")
	public void setUserArea3(String userArea3) {
		this.userArea3 = userArea3;
	}
	@Importable(name="Email Address")
	public void setEmailAddress(String eml) {
		this.emailAddress = eml;
	}
	@Importable(name="Svc City")
	public void setCustCity(String custCity) {
		this.custCity = custCity;
	}
	@Importable(name="Category")
	public void setProductCategory(String productCategory) {
		this.productCategory = productCategory;
	}
	@Importable(name="Svc Country Code")
	public void setCustCountry(String custCountry) {
		this.custCountry = custCountry;
	}
}