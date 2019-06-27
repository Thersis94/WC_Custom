package com.wsla.util.migration.vo;

import java.util.Date;

import com.siliconmtn.annotations.Importable;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <p><b>Title:</b> SOExtendedFileVO.java</p>
 * <p><b>Description:</b> The SOXDD Excel file from Steve.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jun 19, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class SOExtendedFileVO {

	private String soNumber;
	private Date createDate;
	private String swUserId;
	private String equipOwned;
	private String retailer;
	private Date purchaseDate;
	private String isTVMounted;
	private String emailAddress;
	private String sendSurvey;
	private String storeNumber;
	private double costPesos;
	private double costDollars;
	private String bankDetail;
	private Date mfgAuthDate;
	private String storeCreditNote;
	private String mfgDebitMemoToStore;
	private Date mfgReimbursedStore;
	private String mfgDepositDetail;
	private Date storeRcvdDeposit;
	private Date storeRefundedUser;
	private String storeAppliedCreditNote;
	private int popAttached;
	private String partRequired; //goes in part table
	private Date partSentDate; //goes in part table
	private Date partRcvdDate; //goes in part table
	private String productLocation;
	private String exitCode;
	private String shipmentTrackingNumber; //goes in shipment table
	private String partDescription;
	private String shipmentReturnTracking; //shipment table?  credit memo?  (column AD)
	private Date pendingPopEndUserConfDate;
	private Date returnShipmentDate;
	private String productInsured;
	private String productInstructions;
	private String casInvoiceNumber;
	private Date casPaidDate;
	private String primary3Status;
	private String productDisposition;
	private String recordNumber;
	private int communication;
	private int walmartProgam;
	private int service;
	private int quantity;
	private int warrantyProvider;
	private int importer;
	private int information;
	private int cellular;
	private int webinar;
	private int warrantyNumber;
	private int visitNumber;
	private int weekNumber;
	private int cooperate;
	private int anotherTechGo;


	public String getSoNumber() {
		return soNumber;
	}
	public Date getCreateDate() {
		return createDate;
	}
	public String getSwUserId() {
		return swUserId;
	}
	@Column(name="attr_ownsTv")
	public String getEquipOwned() {
		return equipOwned;
	}
	public String getRetailer() {
		return retailer;
	}
	@Column(name="purchaseDate")
	public Date getPurchaseDate() {
		return purchaseDate;
	}
	@Column(name="attr_mounted")
	public String getIsTVMounted() {
		return isTVMounted;
	}
	@Column(name="email")
	public String getEmailAddress() {
		return StringUtil.isValidEmail(emailAddress) ? emailAddress : null;
	}
	@Column(name="attr_emailPermission")
	public String getSendSurvey() {
		return sendSurvey;
	}
	@Column(name="autoRetailerId")
	public String getStoreNumber() {
		return storeNumber;
	}
	public double getCostPesos() {
		return costPesos;
	}
	@Column(name="attr_purchasePrice")
	public double getCostDollars() {
		return costDollars;
	}
	public String getBankDetail() {
		return bankDetail;
	}
	public Date getMfgAuthDate() {
		return mfgAuthDate;
	}
	@Column(name="attr_credit_memo")
	public String getStoreCreditNote() {
		return storeCreditNote;
	}
	public String getMfgDebitMemoToStore() {
		return mfgDebitMemoToStore;
	}
	public Date getMfgReimbursedStore() {
		return mfgReimbursedStore;
	}
	public String getMfgDepositDetail() {
		return mfgDepositDetail;
	}
	public Date getStoreRcvdDeposit() {
		return storeRcvdDeposit;
	}
	public Date getStoreRefundedUser() {
		return storeRefundedUser;
	}
	public String getStoreAppliedCreditNote() {
		return storeAppliedCreditNote;
	}
	@Column(name="attr_proofPurchase")
	public int getPopAttached() {
		return popAttached;
	}
	@Column(name="attr_partsNotes")
	public String getPartRequired() {
		return partRequired;
	}
	public Date getPartSentDate() {
		return partSentDate;
	}
	public Date getPartRcvdDate() {
		return partRcvdDate;
	}
	public String getProductLocation() {
		return productLocation;
	}
	@Column(name="attr_unitDefect")
	public String getExitCode() {
		return exitCode;
	}
	public String getShipmentTrackingNumber() {
		return shipmentTrackingNumber;
	}
	public String getPartDescription() {
		return partDescription;
	}
	public String getShipmentReturnTracking() {
		return shipmentReturnTracking;
	}
	public Date getPendingPopEndUserConfDate() {
		return pendingPopEndUserConfDate;
	}
	public Date getReturnShipmentDate() {
		return returnShipmentDate;
	}
	@Column(name="attr_userFunded")
	public String getProductInsured() {
		return productInsured;
	}
	@Column(name="attr_symptomsComments")
	public String getProductInstructions() {
		return productInstructions;
	}
	public String getCasInvoiceNumber() {
		return casInvoiceNumber;
	}
	public Date getCasPaidDate() {
		return casPaidDate;
	}
	public String getPrimary3Status() {
		return primary3Status;
	}
	@Column(name="attr_dispositionCode")
	public String getProductDisposition() {
		return productDisposition;
	}
	public String getRecordNumber() {
		return recordNumber;
	}
	@Column(name="attr_order_origin")
	public int getCommunication() {
		return communication;
	}
	public int getWalmartProgam() {
		return walmartProgam;
	}
	public int getService() {
		return service;
	}
	public int getQuantity() {
		return quantity;
	}
	public int getWarrantyProvider() {
		return warrantyProvider;
	}
	public int getImporter() {
		return importer;
	}
	public int getInformation() {
		return information;
	}
	public int getCellular() {
		return cellular;
	}
	public int getWebinar() {
		return webinar;
	}
	public int getWarrantyNumber() {
		return warrantyNumber;
	}
	public int getVisitNumber() {
		return visitNumber;
	}
	public int getWeekNumber() {
		return weekNumber;
	}
	public int getCooperate() {
		return cooperate;
	}
	public int getAnotherTechGo() {
		return anotherTechGo;
	}


	/*************  SETTERS **********************/

	@Importable(name="S/O NUMBER")
	public void setSoNumber(String soNumber) {
		this.soNumber = soNumber;
	}
	@Importable(name="Date Created")
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	@Importable(name="SW USER ID")
	public void setSwUserId(String swUserId) {
		this.swUserId = swUserId;
	}
	@Importable(name="EQUIP OWNER")
	public void setEquipOwned(String equipOwned) {
		this.equipOwned = equipOwned;
	}
	@Importable(name="RETAILER")
	public void setRetailer(String retailer) {
		this.retailer = retailer;
	}
	@Importable(name="DATE OF PURCHASE")
	public void setPurchaseDate(Date purchaseDate) {
		this.purchaseDate = purchaseDate;
	}
	@Importable(name="IS TV MOUNTED?")
	public void setIsTVMounted(String isTVMounted) {
		this.isTVMounted = isTVMounted;
	}
	@Importable(name="EMAIL ADDRESS")
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
	@Importable(name="CAN WE SEND SURVEY EMAIL?")
	public void setSendSurvey(String sendSurvey) {
		this.sendSurvey = sendSurvey;
	}
	@Importable(name="STORE NUMBER")
	public void setStoreNumber(String storeNumber) {
		this.storeNumber = storeNumber;
	}
	@Importable(name="COST IN PESOS")
	public void setCostPesos(double costPesos) {
		this.costPesos = costPesos;
	}
	@Importable(name="COST IN USD")
	public void setCostDollars(double costDollars) {
		this.costDollars = costDollars;
	}
	@Importable(name="END USER BANK DETAIL")
	public void setBankDetail(String bankDetail) {
		this.bankDetail = bankDetail;
	}
	@Importable(name="MFG AUTHORIZATION DATE")
	public void setMfgAuthDate(Date mfgAuthDate) {
		this.mfgAuthDate = mfgAuthDate;
	}
	@Importable(name="STORE CREDIT NOTE")
	public void setStoreCreditNote(String storeCreditNote) {
		this.storeCreditNote = storeCreditNote;
	}
	@Importable(name="MFG DEBIT MEMO TO STORE")
	public void setMfgDebitMemoToStore(String mfgDebitMemoToStore) {
		this.mfgDebitMemoToStore = mfgDebitMemoToStore;
	}
	@Importable(name="DATE MFG REIMBURSED STORE")
	public void setMfgReimbursedStore(Date mfgReimbursedStore) {
		this.mfgReimbursedStore = mfgReimbursedStore;
	}
	@Importable(name="MFG DEPOSIT DETAIL")
	public void setMfgDepositDetail(String mfgDepositDetail) {
		this.mfgDepositDetail = mfgDepositDetail;
	}
	@Importable(name="DATE STORE RECEIVED DEPOSIT")
	public void setStoreRcvdDeposit(Date storeRcvdDeposit) {
		this.storeRcvdDeposit = storeRcvdDeposit;
	}
	@Importable(name="DATE STORE REFUNDED END USER")
	public void setStoreRefundedUser(Date storeRefundedUser) {
		this.storeRefundedUser = storeRefundedUser;
	}
	@Importable(name="STORE APPLIED CREDIT NOTE")
	public void setStoreAppliedCreditNote(String storeAppliedCreditNote) {
		this.storeAppliedCreditNote = storeAppliedCreditNote;
	}
	@Importable(name="POP ATTACHED")
	public void setPopAttached(int popAttached) {
		this.popAttached = popAttached;
	}
	@Importable(name="PART OR P/N REQUIRED")
	public void setPartRequired(String partRequired) {
		this.partRequired = partRequired;
	}
	@Importable(name="DATE PART SENT")
	public void setPartSentDate(Date partSentDate) {
		this.partSentDate = partSentDate;
	}
	@Importable(name="DATE CAS RECEIVED PART")
	public void setPartRcvdDate(Date partRcvdDate) {
		this.partRcvdDate = partRcvdDate;
	}
	@Importable(name="PRODUCT LOCATION")
	public void setProductLocation(String productLocation) {
		this.productLocation = productLocation;
	}
	@Importable(name="EXIT CODE")
	public void setExitCode(String exitCode) {
		this.exitCode = exitCode;
	}
	@Importable(name="SHIPMENT TRACKING NUMBER")
	public void setShipmentTrackingNumber(String shipmentTrackingNumber) {
		this.shipmentTrackingNumber = shipmentTrackingNumber;
	}
	@Importable(name="PART DESCRIPTION")
	public void setPartDescription(String partDescription) {
		this.partDescription = partDescription;
	}
	@Importable(name="SHIPMENT TRACKING COMING BACK")
	public void setShipmentReturnTracking(String shipmentReturnTracking) {
		this.shipmentReturnTracking = shipmentReturnTracking;
	}
	@Importable(name="PENDING POP F/END USER")
	public void setPendingPopEndUserConfDate(Date pendingPopEndUserConfDate) {
		this.pendingPopEndUserConfDate = pendingPopEndUserConfDate;
	}
	@Importable(name="DATE OF RETURN SHIPMENT")
	public void setReturnShipmentDate(Date returnShipmentDate) {
		this.returnShipmentDate = returnShipmentDate;
	}
	@Importable(name="PRODUCT INSURED")
	public void setProductInsured(String productInsured) {
		this.productInsured = productInsured;
	}
	@Importable(name="PRODUCT INSTRUCTIONS")
	public void setProductInstructions(String productInstructions) {
		this.productInstructions = productInstructions;
	}
	@Importable(name="CAS INVOICE NUMBER")
	public void setCasInvoiceNumber(String casInvoiceNumber) {
		this.casInvoiceNumber = casInvoiceNumber;
	}
	@Importable(name="DATE CAS PAID")
	public void setCasPaidDate(Date casPaidDate) {
		this.casPaidDate = casPaidDate;
	}
	@Importable(name="PRIMARY 3 STATUS")
	public void setPrimary3Status(String primary3Status) {
		this.primary3Status = primary3Status;
	}
	@Importable(name="PRODUCT DISPOSITION")
	public void setProductDisposition(String productDisposition) {
		this.productDisposition = productDisposition;
	}
	@Importable(name="RECORD NUMBER")
	public void setRecordNumber(String recordNumber) {
		this.recordNumber = recordNumber;
	}
	@Importable(name="COMMUNICATION")
	public void setCommunication(int communication) {
		this.communication = communication;
	}
	@Importable(name="WALMART PROGRAM")
	public void setWalmartProgam(int walmartProgam) {
		this.walmartProgam = walmartProgam;
	}
	@Importable(name="SERVICES")
	public void setService(int service) {
		this.service = service;
	}
	@Importable(name="QUANTITY")
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	@Importable(name="WARRANTY PROVIDER")
	public void setWarrantyProvider(int warrantyProvider) {
		this.warrantyProvider = warrantyProvider;
	}
	@Importable(name="IMPORTER")
	public void setImporter(int importer) {
		this.importer = importer;
	}
	@Importable(name="INFORMATION")
	public void setInformation(int information) {
		this.information = information;
	}
	@Importable(name="CELLULAR")
	public void setCellular(int cellular) {
		this.cellular = cellular;
	}
	@Importable(name="WEBINAR")
	public void setWebinar(int webinar) {
		this.webinar = webinar;
	}
	@Importable(name="WARRANTY NUMBER")
	public void setWarrantyNumber(int warrantyNumber) {
		this.warrantyNumber = warrantyNumber;
	}
	@Importable(name="VISIT NUMBER")
	public void setVisitNumber(int visitNumber) {
		this.visitNumber = visitNumber;
	}
	@Importable(name="WEEK NUMBER")
	public void setWeekNumber(int weekNumber) {
		this.weekNumber = weekNumber;
	}
	@Importable(name="COOPERATE?")
	public void setCooperate(int cooperate) {
		this.cooperate = cooperate;
	}
	@Importable(name="ANOTHER TECH WILL GO")
	public void setAnotherTechGo(int anotherTechGo) {
		this.anotherTechGo = anotherTechGo;
	}
}
