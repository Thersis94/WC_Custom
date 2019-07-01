package com.wsla.data.admin;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs 3.5
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;

/****************************************************************************
 * <b>Title</b>: RefundVerificationVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object storing a row of data for the user refund
 * verification data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since May 29, 2019
 * @updates:
 ****************************************************************************/
public class RefundVerificationVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4579275939221683090L;

	// Members
	private String ticketId;
	private String ticketNumber;
	private String dataEntryId;
	private String firstName;
	private String lastName;
	private String phoneNumber;
	private String email;
	private double refundAmount;
	private Date approvalDate;
	
	/**
	 * 
	 */
	public RefundVerificationVO() {
		super();
	}

	/**
	 * @param req
	 */
	public RefundVerificationVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public RefundVerificationVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the ticketId
	 */
	@Column(name="ticket_id", isPrimaryKey=true)
	public String getTicketId() {
		return ticketId;
	}

	/**
	 * @return the ticketNumber
	 */
	@Column(name="ticket_no")
	public String getTicketNumber() {
		return ticketNumber;
	}

	/**
	 * @return the firstName
	 */
	@Column(name="first_nm")
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @return the lastName
	 */
	@Column(name="last_nm")
	public String getLastName() {
		return lastName;
	}

	/**
	 * @return the phoneNumber
	 */
	@Column(name="main_phone_txt")
	public String getPhoneNumber() {
		return phoneNumber;
	}

	/**
	 * @return the email
	 */
	@Column(name="email_address_txt")
	public String getEmail() {
		return email;
	}

	/**
	 * @return the refundAmount
	 */
	@Column(name="refund_amount_no")
	public double getRefundAmount() {
		return refundAmount;
	}

	/**
	 * @return the approvalDate
	 */
	@Column(name="approval_dt")
	public Date getApprovalDate() {
		return approvalDate;
	}

	/**
	 * @return the ticketDataId
	 */
	@Column(name="data_entry_id")
	public String getDataEntryId() {
		return dataEntryId;
	}

	/**
	 * @param ticketId the ticketId to set
	 */
	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	/**
	 * @param ticketNumber the ticketNumber to set
	 */
	public void setTicketNumber(String ticketNumber) {
		this.ticketNumber = ticketNumber;
	}

	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * @param phoneNumber the phoneNumber to set
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @param refundAmount the refundAmount to set
	 */
	public void setRefundAmount(double refundAmount) {
		this.refundAmount = refundAmount;
	}

	/**
	 * @param approvalDate the approvalDate to set
	 */
	public void setApprovalDate(Date approvalDate) {
		this.approvalDate = approvalDate;
	}

	/**
	 * @param dataEntryId the ticketDataId to set
	 */
	public void setDataEntryId(String dataEntryId) {
		this.dataEntryId = dataEntryId;
	}

}
