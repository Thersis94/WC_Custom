package com.wsla.util.migration.archive;

import java.util.Date;

import com.siliconmtn.annotations.Importable;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <p><b>Title:</b> SOXDDFileVO.java</p>
 * <p><b>Description:</b> The SOXDD Excel file from Steve.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jun 19, 2019
 * <b>Changes:</b>
 ****************************************************************************/
@Table(name="wsla_sw_xdd")
public class SOXDDFileVO {

	private String soNumber;
	private Date createDate;
	private String status;
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
	private String actionCode;
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
	private String fileName;


	@Column(name="so_number", isPrimaryKey=true)
	public String getSoNumber() {
		return soNumber;
	}
	@Column(name="date_created")
	public Date getCreateDate() {
		return createDate;
	}
	@Column(name="sw_user_id")
	public String getSwUserId() {
		return swUserId;
	}
	@Column(name="equip_owner")
	public String getEquipOwned() {
		return equipOwned;
	}
	@Column(name="retailer")
	public String getRetailer() {
		return retailer;
	}
	@Column(name="date_of_purchase")
	public Date getPurchaseDate() {
		return purchaseDate;
	}
	@Column(name="is_tv_mounted")
	public String getIsTVMounted() {
		return isTVMounted;
	}
	@Column(name="email_address")
	public String getEmailAddress() {
		return emailAddress;
	}
	@Column(name="can_we_send_survey_email")
	public String getSendSurvey() {
		return sendSurvey;
	}
	@Column(name="store_number")
	public String getStoreNumber() {
		return storeNumber;
	}
	@Column(name="cost_in_pesos")
	public double getCostPesos() {
		return costPesos;
	}
	@Column(name="cost_in_usd")
	public double getCostDollars() {
		return costDollars;
	}
	@Column(name="end_user_bank_detail")
	public String getBankDetail() {
		return bankDetail;
	}
	@Column(name="mfg_authorization_date")
	public Date getMfgAuthDate() {
		return mfgAuthDate;
	}
	@Column(name="store_credit_note")
	public String getStoreCreditNote() {
		return storeCreditNote;
	}
	@Column(name="mfg_debit_memo_to_store")
	public String getMfgDebitMemoToStore() {
		return mfgDebitMemoToStore;
	}
	@Column(name="date_mfg_reimbursed_store")
	public Date getMfgReimbursedStore() {
		return mfgReimbursedStore;
	}
	@Column(name="mfg_deposit_detail")
	public String getMfgDepositDetail() {
		return mfgDepositDetail;
	}
	@Column(name="date_store_received_deposit")
	public Date getStoreRcvdDeposit() {
		return storeRcvdDeposit;
	}
	@Column(name="date_store_refunded_end_user")
	public Date getStoreRefundedUser() {
		return storeRefundedUser;
	}
	@Column(name="store_applied_credit_note")
	public String getStoreAppliedCreditNote() {
		return storeAppliedCreditNote;
	}
	@Column(name="pop_attached")
	public int getPopAttached() {
		return popAttached;
	}
	@Column(name="part_or_pn_required")
	public String getPartRequired() {
		return partRequired;
	}
	@Column(name="date_part_sent")
	public Date getPartSentDate() {
		return partSentDate;
	}
	@Column(name="date_cas_received_part")
	public Date getPartRcvdDate() {
		return partRcvdDate;
	}
	@Column(name="product_location")
	public String getProductLocation() {
		return productLocation;
	}
	@Column(name="action_code_1")
	public String getActionCode() {
		return actionCode;
	}
	@Column(name="exit_code")
	public String getExitCode() {
		return exitCode;
	}
	@Column(name="shipment_tracking_number")
	public String getShipmentTrackingNumber() {
		return shipmentTrackingNumber;
	}
	@Column(name="part_description")
	public String getPartDescription() {
		return partDescription;
	}
	@Column(name="shipment_tracking_coming_back")
	public String getShipmentReturnTracking() {
		return shipmentReturnTracking;
	}
	@Column(name="pending_pop_fend_user")
	public Date getPendingPopEndUserConfDate() {
		return pendingPopEndUserConfDate;
	}
	@Column(name="date_of_return_shipment")
	public Date getReturnShipmentDate() {
		return returnShipmentDate;
	}
	@Column(name="product_insured")
	public String getProductInsured() {
		return productInsured;
	}
	@Column(name="product_instructions")
	public String getProductInstructions() {
		return productInstructions;
	}
	@Column(name="cas_invoice_number")
	public String getCasInvoiceNumber() {
		return casInvoiceNumber;
	}
	@Column(name="date_cas_paid")
	public Date getCasPaidDate() {
		return casPaidDate;
	}
	@Column(name="primary_3_status")
	public String getPrimary3Status() {
		return primary3Status;
	}
	@Column(name="product_disposition")
	public String getProductDisposition() {
		return productDisposition;
	}
	@Column(name="record_number")
	public String getRecordNumber() {
		return recordNumber;
	}
	@Column(name="communication")
	public int getCommunication() {
		return communication;
	}
	@Column(name="walmart_program")
	public int getWalmartProgam() {
		return walmartProgam;
	}
	@Column(name="services")
	public int getService() {
		return service;
	}
	@Column(name="quantity")
	public int getQuantity() {
		return quantity;
	}
	@Column(name="warranty_provider")
	public int getWarrantyProvider() {
		return warrantyProvider;
	}
	@Column(name="importer")
	public int getImporter() {
		return importer;
	}
	@Column(name="information")
	public int getInformation() {
		return information;
	}
	@Column(name="cellular")
	public int getCellular() {
		return cellular;
	}
	@Column(name="webinar")
	public int getWebinar() {
		return webinar;
	}
	@Column(name="warranty_number")
	public int getWarrantyNumber() {
		return warrantyNumber;
	}
	@Column(name="visit_number")
	public int getVisitNumber() {
		return visitNumber;
	}
	@Column(name="week_number")
	public int getWeekNumber() {
		return weekNumber;
	}
	@Column(name="cooperate")
	public int getCooperate() {
		return cooperate;
	}
	@Column(name="another_tech_will_go")
	public int getAnotherTechGo() {
		return anotherTechGo;
	}
	@Column(name="status")
	public String getStatus() {
		return status;
	}
	@Column(name="file_name")
	public String getFileName() {
		return fileName;
	}


	@Importable(name="S/O NUMBER")
	public void setSoNumber(String soNumber) {
		this.soNumber = soNumber;
	}
	@Importable(name="Date Created")
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	@Importable(name="Status")
	public void setStatus(String status) {
		this.status = status;
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
		if ("N".equalsIgnoreCase(storeCreditNote)) return;
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
	@Importable(name="Action Code 1")
	public void setActionCode(String actionCode) {
		this.actionCode = actionCode;
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
	public void setFileName(String nm) {
		this.fileName = nm;
	}
}
