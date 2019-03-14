package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: TicketScheduleVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for a schedule record on a ticket
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Oct 23, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_ticket_schedule")
public class TicketScheduleVO extends BeanDataVO {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5696588888542499393L;
	
	/**
	 * Options for the type of Schedule
	 */
	public enum TypeCode {
		PICKUP, DROPOFF;
	}

	// Member Variables
	private String ticketScheduleId;
	private String ticketId;
	private String ticketNumber;
	private String ledgerEntryId;
	private String casLocationId;
	private String ownerLocationId;
	private TypeCode typeCode;
	private String signerName;
	private String signatureText;
	private int productValidatedFlag;
	private String notesText;
	private String recordTypeCode;
	private Date createDate;
	private Date updateDate;
	private Date scheduleDate;
	private Date completeDate;
	private Date startDate;
	private Date endDate;
	
	// Bean Sub-Elements
	private TicketAssignmentVO casLocation;
	private TicketAssignmentVO ownerLocation;
	private TicketLedgerVO ledger;

	/**
	 * 
	 */
	public TicketScheduleVO() {
		super();
	}

	/**
	 * @param req
	 */
	public TicketScheduleVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public TicketScheduleVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the ticketScheduleId
	 */
	@Column(name="ticket_schedule_id", isPrimaryKey=true)
	public String getTicketScheduleId() {
		return ticketScheduleId;
	}

	/**
	 * @param ticketScheduleId the ticketScheduleId to set
	 */
	public void setTicketScheduleId(String ticketScheduleId) {
		this.ticketScheduleId = ticketScheduleId;
	}

	/**
	 * @return the ticketId
	 */
	@Column(name="ticket_id")
	public String getTicketId() {
		return ticketId;
	}

	/**
	 * @param ticketId the ticketId to set
	 */
	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	/**
	 * @return the ledgerEntryId
	 */
	@Column(name="ledger_entry_id")
	public String getLedgerEntryId() {
		return ledgerEntryId;
	}

	/**
	 * @param ledgerEntryId the ledgerEntryId to set
	 */
	public void setLedgerEntryId(String ledgerEntryId) {
		this.ledgerEntryId = ledgerEntryId;
	}

	/**
	 * @return the casLocationId
	 */
	@Column(name="cas_location_id")
	public String getCasLocationId() {
		return casLocationId;
	}

	/**
	 * @param casLocationId the casLocationId to set
	 */
	public void setCasLocationId(String casLocationId) {
		this.casLocationId = casLocationId;
	}

	/**
	 * @return the ownerLocationId
	 */
	@Column(name="owner_location_id")
	public String getOwnerLocationId() {
		return ownerLocationId;
	}

	/**
	 * @param ownerLocationId the ownerLocationId to set
	 */
	public void setOwnerLocationId(String ownerLocationId) {
		this.ownerLocationId = ownerLocationId;
	}

	/**
	 * @return the typeCode
	 */
	@Column(name="transfer_type_cd")
	public TypeCode getTypeCode() {
		return typeCode;
	}

	/**
	 * @param typeCode the typeCode to set
	 */
	public void setTypeCode(TypeCode typeCode) {
		this.typeCode = typeCode;
	}

	/**
	 * @return the signerName
	 */
	@Column(name="signer_nm")
	public String getSignerName() {
		return signerName;
	}

	/**
	 * @param signerName the signerName to set
	 */
	public void setSignerName(String signerName) {
		this.signerName = signerName;
	}

	/**
	 * @return the signatureText
	 */
	@Column(name="signature_txt")
	public String getSignatureText() {
		return signatureText;
	}

	/**
	 * @param signatureText the signatureText to set
	 */
	public void setSignatureText(String signatureText) {
		this.signatureText = signatureText;
	}

	/**
	 * @return the productValidatedFlag
	 */
	@Column(name="product_validated_flg")
	public int getProductValidatedFlag() {
		return productValidatedFlag;
	}

	/**
	 * @param productValidatedFlag the productValidatedFlag to set
	 */
	public void setProductValidatedFlag(int productValidatedFlag) {
		this.productValidatedFlag = productValidatedFlag;
	}

	/**
	 * @return the notesText
	 */
	@Column(name="notes_txt")
	public String getNotesText() {
		return notesText;
	}

	/**
	 * @param notesText the notesText to set
	 */
	public void setNotesText(String notesText) {
		this.notesText = notesText;
	}

	/**
	 * @return the recordTypeCode
	 */
	@Column(name="record_type_cd")
	public String getRecordTypeCode() {
		return recordTypeCode;
	}

	/**
	 * @param recordTypeCode the recordTypeCode to set
	 */
	public void setRecordTypeCode(String recordTypeCode) {
		this.recordTypeCode = recordTypeCode;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @return the scheduleDate
	 */
	@Column(name="schedule_dt")
	public Date getScheduleDate() {
		return scheduleDate;
	}

	/**
	 * @param scheduleDate the scheduleDate to set
	 */
	public void setScheduleDate(Date scheduleDate) {
		this.scheduleDate = scheduleDate;
	}

	/**
	 * @return the completeDate
	 */
	@Column(name="complete_dt")
	public Date getCompleteDate() {
		return completeDate;
	}

	/**
	 * @param completeDate the completeDate to set
	 */
	public void setCompleteDate(Date completeDate) {
		this.completeDate = completeDate;
	}

	/**
	 * @return the casLocation
	 */
	public TicketAssignmentVO getCasLocation() {
		return casLocation;
	}

	/**
	 * @param casLocation the casLocation to set
	 */
	@BeanSubElement
	public void setCasLocation(TicketAssignmentVO casLocation) {
		this.casLocation = casLocation ;
	}

	/**
	 * @return the ownerLocation
	 */
	public TicketAssignmentVO getOwnerLocation() {
		return ownerLocation;
	}

	/**
	 * @param ownerLocation the ownerLocation to set
	 */
	@BeanSubElement
	public void setOwnerLocation(TicketAssignmentVO ownerLocation) {
		this.ownerLocation =  ownerLocation;
	}

	/**
	 * @return the ledger
	 */
	public TicketLedgerVO getLedger() {
		return ledger;
	}

	/**
	 * @param ledger the ledger to set
	 */
	@BeanSubElement
	public void setLedger(TicketLedgerVO ledger) {
		this.ledger = ledger;
	}

	/**
	 * @return the ticketNumber
	 */
	@Column(name="ticket_no", isReadOnly=true)
	public String getTicketNumber() {
		return ticketNumber;
	}

	/**
	 * @param ticketNumber the ticketNumber to set
	 */
	public void setTicketNumber(String ticketNumber) {
		this.ticketNumber = ticketNumber;
	}

	/**
	 * @return the startDate
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @param startDate the startDate to set
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @return the endDate
	 */
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
}

