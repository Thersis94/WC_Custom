package com.wsla.util.migration.archive;

import java.util.Date;

import com.siliconmtn.annotations.Importable;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <p><b>Title:</b> SOHeaderFileVO.java</p>
 * <p><b>Description:</b> The SOHDR Excel file from Steve - unedited for DB archival.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Oct 11, 2019
 * <b>Changes:</b>
 ****************************************************************************/
@Table(name="wsla_sw_hdr")
public class SOHDRFileVO {

	private String soNumber;
	private String origSoNumber;
	private Date receivedDate;
	private Date closedDate;
	private String serviceTech;
	private String territory;
	private Date altKeyDate;
	private String equipmentId;
	private String manufacturer;
	private String serialNumber;
	private String soType;
	private String custName;
	private String custAddress1;
	private String custAddress2;
	private String custCity;
	private String custState;
	private String custZip;
	private String custCountry;
	private String custPhone;
	private String custContact;
	private String problemCode; 
	private String coverageCode;
	private String custPO;
	private String operator;
	private String receivedMethod;
	private String status;
	private Date startDate;
	private Date statusDate;
	private String userArea1;
	private String userArea2;
	private String userArea3;
	private String emailAddress;
	private String productCategory;
	private String actionCode1;
	private String fileName;

	@Column(name="so_number", isPrimaryKey=true)
	public String getSoNumber() {
		return soNumber;
	}
	@Column(name="original_so")
	public String getOriginalSoNumber() {
		return origSoNumber;
	}
	@Column(name="date_received")
	public Date getReceivedDate() {
		return receivedDate;
	}
	@Column(name="date_closed")
	public Date getClosedDate() {
		return closedDate;
	}
	@Column(name="service_tech")
	public String getServiceTech() {
		return serviceTech;
	}
	@Column(name="territory")
	public String getTerritory() {
		return territory;
	}
	@Column(name="date_altkey_recd")
	public Date getAltKeyDate() {
		return altKeyDate;
	}
	@Column(name="equipment_id")
	public String getEquipmentId() {
		return equipmentId;
	}
	@Column(name="manufacturer")
	public String getManufacturer() {
		return manufacturer;
	}
	@Column(name="serial_number")
	public String getSerialNumber() {
		return serialNumber;
	}
	@Column(name="svc_order_type")
	public String getSoType() {
		return soType;
	}
	@Column(name="name")
	public String getCustName() {
		return custName;
	}
	@Column(name="svc_address_1")
	public String getCustAddress1() {
		return custAddress1;
	}
	@Column(name="svc_address_2")
	public String getCustAddress2() {
		return custAddress2;
	}
	@Column(name="svc_st")
	public String getCustState() {
		return custState;
	}
	@Column(name="svc_zip")
	public String getCustZip() {
		//flush all zeros
		return custZip;
	}
	@Column(name="phone_number")
	public String getCustPhone() {
		return custPhone;
	}
	@Column(name="contact")
	public String getCustContact() {
		return custContact;
	}
	@Column(name="problem_code")
	public String getProblemCode() {
		return problemCode;
	}
	@Column(name="coverage_code")
	public String getCoverageCode() {
		return coverageCode;
	}
	@Column(name="customer_p_o")
	public String getCustPO() {
		return custPO;
	}
	@Column(name="operator")
	public String getOperator() {
		return operator;
	}
	@Column(name="received_method")
	public String getReceivedMethod() {
		return receivedMethod;
	}
	@Column(name="status")
	public String getStatus() {
		return status;
	}
	@Column(name="date_started")
	public Date getStartDate() {
		return startDate;
	}
	@Column(name="status_date")
	public Date getStatusDate() {
		return statusDate;
	}
	@Column(name="user_area_1")
	public String getUserArea1() {
		return userArea1;
	}
	@Column(name="user_area_2")
	public String getUserArea2() {
		return userArea2;
	}
	@Column(name="user_area_3")
	public String getUserArea3() {
		return userArea3;
	}	
	@Column(name="email_address")
	public String getEmailAddress() {
		return emailAddress;
	}
	@Column(name="svc_city")
	public String getCustCity() {
		return custCity;
	}
	@Column(name="category")
	public String getProductCategory() {
		return productCategory;
	}
	@Column(name="svc_country_code")
	public String getCustCountry() {
		return custCountry;
	}
	@Column(name="action_code_1")
	public String getActionCode1() {
		return actionCode1;
	}
	@Column(name="file_name")
	public String getFileName() {
		return fileName;
	}


	@Importable(name="SO Number")
	public void setSoNumber(String soNumber) {
		this.soNumber = soNumber;
	}
	@Importable(name="Original SO Number")
	public void setOriginalSoNumber(String origSoNumber) {
		this.origSoNumber = origSoNumber;
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
	@Importable(name="Action Code 1")
	public void setActionCode1(String cd) {
		this.actionCode1 = cd;
	}
	public void setFileName(String nm) {
		this.fileName = nm;
	}
}