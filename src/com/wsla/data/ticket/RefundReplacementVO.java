package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.ticket.RefundMemoVO.MemoType;

/****************************************************************************
 * <b>Title</b>: RefundReplacementVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value Object for the Refund and Replacement data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 2, 2019
 * @updates:
 ****************************************************************************/
@Table(name="wsla_ticket_ref_rep")
public class RefundReplacementVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4287896974164348281L;
	
	// String Member Variables
	private String refundReplacementId;
	private String ticketId;
	private String approvalType;
	private String unitDisposition;
	private String rejectComment;
	private String unitReceivedById;
	private String replacementLocationId;
	private String replacementProductId;
	private String shipmentId;
	
	// Numeric Member Variables
	private int backOrderFlag;
	private double refundAmount;
	
	// Date Member Variables
	private Date unitReceiveDate;
	private Date createDate;
	private Date updateDate;
	
	// Sub-elements
	private List<RefundMemoVO> memos = new ArrayList<>();
	
	/**
	 * 
	 */
	public RefundReplacementVO() {
		super();
	}

	/**
	 * @param req
	 */
	public RefundReplacementVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public RefundReplacementVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * Helper method to get a credit memo type
	 * @return
	 */
	public RefundMemoVO getCreditMemo() {
		for (RefundMemoVO memo : memos) {
			if (MemoType.CREDIT.equals(memo.getMemoType())) return memo;
		}
		
		return null;
	}
	
	/**
	 * Helper method to get a debit memo type
	 * @return
	 */
	public RefundMemoVO getDebitMemo() {
		for (RefundMemoVO memo : memos) {
			if (MemoType.DEBIT.equals(memo.getMemoType())) return memo;
		}
		return null;
	}

	/**
	 * @return the refundReplacementId
	 */
	@Column(name="ticket_ref_rep_id", isPrimaryKey=true)
	public String getRefundReplacementId() {
		return refundReplacementId;
	}

	/**
	 * @return the ticketId
	 */
	@Column(name="ticket_id")
	public String getTicketId() {
		return ticketId;
	}

	/**
	 * @return the approvalType
	 */
	@Column(name="approval_type_cd")
	public String getApprovalType() {
		return approvalType;
	}

	/**
	 * @return the unitDisposition
	 */
	@Column(name="unit_disposition_cd")
	public String getUnitDisposition() {
		return unitDisposition;
	}

	/**
	 * @return the rejectComment
	 */
	@Column(name="reject_cmmt")
	public String getRejectComment() {
		return rejectComment;
	}

	/**
	 * @return the unitReceivedById
	 */
	@Column(name="unit_rcvd_by_id")
	public String getUnitReceivedById() {
		return unitReceivedById;
	}

	/**
	 * @return the replacementLocaitonId
	 */
	@Column(name="replacement_location_id")
	public String getReplacementLocationId() {
		return replacementLocationId;
	}

	/**
	 * @return the replacementProductId
	 */
	@Column(name="replacement_product_id")
	public String getReplacementProductId() {
		return replacementProductId;
	}

	/**
	 * @return the shipmentId
	 */
	@Column(name="shipment_id")
	public String getShipmentId() {
		return shipmentId;
	}

	/**
	 * @return the backOrderFlag
	 */
	@Column(name="back_order_flg")
	public int getBackOrderFlag() {
		return backOrderFlag;
	}

	/**
	 * @return the refundAmount
	 */
	@Column(name="refund_amount_no")
	public double getRefundAmount() {
		return refundAmount;
	}

	/**
	 * @return the unitReceiveDate
	 */
	@Column(name="unit_recv_dt")
	public Date getUnitReceiveDate() {
		return unitReceiveDate;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the memos
	 */
	public List<RefundMemoVO> getMemos() {
		return memos;
	}

	/**
	 * @param refundReplacementId the refundReplacementId to set
	 */
	public void setRefundReplacementId(String refundReplacementId) {
		this.refundReplacementId = refundReplacementId;
	}

	/**
	 * @param ticketId the ticketId to set
	 */
	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	/**
	 * @param approvalType the approvalType to set
	 */
	public void setApprovalType(String approvalType) {
		this.approvalType = approvalType;
	}

	/**
	 * @param unitDisposition the unitDisposition to set
	 */
	public void setUnitDisposition(String unitDisposition) {
		this.unitDisposition = unitDisposition;
	}

	/**
	 * @param rejectComment the rejectComment to set
	 */
	public void setRejectComment(String rejectComment) {
		this.rejectComment = rejectComment;
	}

	/**
	 * @param unitReceivedById the unitReceivedById to set
	 */
	public void setUnitReceivedById(String unitReceivedById) {
		this.unitReceivedById = unitReceivedById;
	}

	/**
	 * @param replacementLocaitonId the replacementLocaitonId to set
	 */
	public void setReplacementLocationId(String replacementLocationId) {
		this.replacementLocationId = replacementLocationId;
	}

	/**
	 * @param replacementProductId the replacementProductId to set
	 */
	public void setReplacementProductId(String replacementProductId) {
		this.replacementProductId = replacementProductId;
	}

	/**
	 * @param shipmentId the shipmentId to set
	 */
	public void setShipmentId(String shipmentId) {
		this.shipmentId = shipmentId;
	}

	/**
	 * @param backOrderFlag the backOrderFlag to set
	 */
	public void setBackOrderFlag(int backOrderFlag) {
		this.backOrderFlag = backOrderFlag;
	}

	/**
	 * @param refundAmount the refundAmount to set
	 */
	public void setRefundAmount(double refundAmount) {
		this.refundAmount = refundAmount;
	}

	/**
	 * @param unitReceiveDate the unitReceiveDate to set
	 */
	public void setUnitReceiveDate(Date unitReceiveDate) {
		this.unitReceiveDate = unitReceiveDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @param memos the memos to set
	 */
	public void setMemos(List<RefundMemoVO> memos) {
		this.memos = memos;
	}

	/**
	 * Add a memo
	 * @param memo
	 */
	@BeanSubElement
	public void addMemo(RefundMemoVO memo) {
		if (memo != null && ! StringUtil.isEmpty(memo.getRefundMemoId()))
			memos.add(memo);
	}
}

