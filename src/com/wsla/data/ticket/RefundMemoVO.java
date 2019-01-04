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
 * <b>Title</b>: RefundMemoVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value Object storing the information on a debit or credit memo
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 2, 2019
 * @updates:
 ****************************************************************************/
@Table(name="wsla_refund_memo")
public class RefundMemoVO extends BeanDataVO {

	/**
	 * Defines the memo types
	 */
	public enum MemoType {
		CREDIT, DEBIT
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3088915496598255415L;
	
	// Member variables
	private String refundMemoId;
	private String refundReplacementId;
	private MemoType memoType;
	private String assetId;
	private String approvalFlag;
	private Date createDate;
	private Date updateDate;
	
	// Sub-beans
	private TicketDataVO asset = new TicketDataVO();

	/**
	 * 
	 */
	public RefundMemoVO() {
		super();
	}

	/**
	 * @param req
	 */
	public RefundMemoVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public RefundMemoVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the refundMemoId
	 */
	@Column(name="refund_memo_id", isPrimaryKey=true)
	public String getRefundMemoId() {
		return refundMemoId;
	}

	/**
	 * @return the refundReplacementId
	 */
	@Column(name="ticket_ref_rep_id")
	public String getRefundReplacementId() {
		return refundReplacementId;
	}

	/**
	 * @return the memoType
	 */
	@Column(name="memo_type_cd")
	public MemoType getMemoType() {
		return memoType;
	}

	/**
	 * @return the assetId
	 */
	@Column(name="asset_id")
	public String getAssetId() {
		return assetId;
	}

	/**
	 * @return the approvalFlag
	 */
	@Column(name="approval_flg")
	public String getApprovalFlag() {
		return approvalFlag;
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
	 * @return the asset
	 */
	public TicketDataVO getAsset() {
		return asset;
	}

	/**
	 * @param refundMemoId the refundMemoId to set
	 */
	public void setRefundMemoId(String refundMemoId) {
		this.refundMemoId = refundMemoId;
	}

	/**
	 * @param refundReplacementId the refundReplacementId to set
	 */
	public void setRefundReplacementId(String refundReplacementId) {
		this.refundReplacementId = refundReplacementId;
	}

	/**
	 * @param memoType the memoType to set
	 */
	public void setMemoType(MemoType memoType) {
		this.memoType = memoType;
	}

	/**
	 * @param assetId the assetId to set
	 */
	public void setAssetId(String assetId) {
		this.assetId = assetId;
	}

	/**
	 * @param approvalFlag the approvalFlag to set
	 */
	public void setApprovalFlag(String approvalFlag) {
		this.approvalFlag = approvalFlag;
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
	 * @param asset the asset to set
	 */
	@BeanSubElement
	public void setAsset(TicketDataVO asset) {
		this.asset = asset;
	}

}

