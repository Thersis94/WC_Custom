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
 * <b>Title</b>: CreditMemoVO.java
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
@Table(name="wsla_credit_memo")
public class CreditMemoVO extends BeanDataVO {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3088915496598255415L;
	
	// Member variables
	private String creditMemoId;
	private String refundReplacementId;
	private String customerMemoCode;
	private String debitMemoCode;
	private String debitMemoId;
	private String assetId;
	private String approvalFlag;
	private String customerAssistedCode;
	
	private String bankName;
	private String accountNumber;
	private String transferCode;
	private double unitCost;
	
	private String approvedBy;
	private double refundAmount;
	private Date approvalDate;
	private Date createDate;
	private Date updateDate;
	private Date authorizationDate;
	private int endUserRefundFlag;
	
	// Sub-beans
	private TicketDataVO asset = new TicketDataVO();
	private DebitMemoVO debitMemo = new DebitMemoVO();
	
	// Read-Only Members
	private String ticketIdText;
	private String filePathUrl;
	private String productName;
	private String ticketId;

	/**
	 * Standing of the ticket in relation to how its progressing through the workflow
	 */
	public enum CustomerAssistedCode {
		STORE_OWNED_REFUND, MERMA
	}
	
	/**
	 * 
	 */
	public CreditMemoVO() {
		super();
	}

	/**
	 * @param req
	 */
	public CreditMemoVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public CreditMemoVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the refundMemoId
	 */
	@Column(name="credit_memo_id", isPrimaryKey=true)
	public String getCreditMemoId() {
		return creditMemoId;
	}

	/**
	 * @return the refundReplacementId
	 */
	@Column(name="ticket_ref_rep_id")
	public String getRefundReplacementId() {
		return refundReplacementId;
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
	 * @return the customerMemoCode
	 */
	@Column(name="customer_memo_cd")
	public String getCustomerMemoCode() {
		return customerMemoCode;
	}

	/**
	 * @return the debitMemoId
	 */
	@Column(name="debit_memo_id")
	public String getDebitMemoId() {
		return debitMemoId;
	}

	/**
	 * @return the approvedBy
	 */
	@Column(name="approved_by_txt")
	public String getApprovedBy() {
		return approvedBy;
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
	 * @return the ticketIdText
	 */
	@Column(name="ticket_no", isReadOnly=true)
	public String getTicketIdText() {
		return ticketIdText;
	}

	/**
	 * @return the filePathUrl
	 */
	@Column(name="file_path_url", isReadOnly=true)
	public String getFilePathUrl() {
		return filePathUrl;
	}

	/**
	 * @return the productName
	 */
	@Column(name="product_nm", isReadOnly=true)
	public String getProductName() {
		return productName;
	}

	/**
	 * @return the asset
	 */
	public TicketDataVO getAsset() {
		return asset;
	}

	/**
	 * @return the debitMemo
	 */
	public DebitMemoVO getDebitMemo() {
		return debitMemo;
	}

	/**
	 * @return the ticketId
	 */
	@Column(name="ticket_id", isReadOnly=true)
	public String getTicketId() {
		return ticketId;
	}

	/**
	 * @return the bankName
	 */
	@Column(name="bank_nm")
	public String getBankName() {
		return bankName;
	}

	/**
	 * @return the accountNumber
	 */
	@Column(name="account_no")
	public String getAccountNumber() {
		return accountNumber;
	}

	/**
	 * @return the transferCode
	 */
	@Column(name="transfer_cd")
	public String getTransferCode() {
		return transferCode;
	}

	/**
	 * @return the unitCost
	 */
	@Column(name="msrp_cost_no", isReadOnly=true)
	public double getUnitCost() {
		return unitCost;
	}

	/**
	 * @return the authenticationDate
	 */
	@Column(name="authorization_dt")
	public Date getAuthorizationDate() {
		return authorizationDate;
	}
	
	/**
	 * @return
	 */
	@Column(name="customer_assisted_cd")
	public String getCustomerAssistedCode() {
		return customerAssistedCode;
	}

	/**
	 * @return
	 */
	@Column(name="end_user_refund_flg")
	public int getEndUserRefundFlag() {
		return endUserRefundFlag;
	}

	/**
	 * @param endUserRefundFlag
	 */
	public void setEndUserRefundFlag(int endUserRefundFlag) {
		this.endUserRefundFlag = endUserRefundFlag;
	}

	/**
	 * @param customerAssistedCode
	 */
	public void setCustomerAssistedCode(String customerAssistedCode) {
		this.customerAssistedCode = customerAssistedCode;
	}
	
	/**
	 * @param customerAssistedCode
	 */
	public void setCustomerAssistedCode(CustomerAssistedCode customerAssistedCode) {
		this.customerAssistedCode = customerAssistedCode.name();
	}

	/**
	 * @param authenticationDate the authenticationDate to set
	 */
	public void setAuthorizationDate(Date authorizationDate) {
		this.authorizationDate = authorizationDate;
	}

	/**
	 * @param unitCost the unitCost to set
	 */
	public void setUnitCost(double unitCost) {
		this.unitCost = unitCost;
	}

	/**
	 * @param transferCode the transferCode to set
	 */
	public void setTransferCode(String transferCode) {
		this.transferCode = transferCode;
	}

	/**
	 * @param accountNumber the accountNumber to set
	 */
	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	/**
	 * @param bankName the bankName to set
	 */
	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	/**
	 * @param ticketId the ticketId to set
	 */
	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	/**
	 * @param refundMemoId the refundMemoId to set
	 */
	public void setCreditMemoId(String creditMemoId) {
		this.creditMemoId = creditMemoId;
	}

	/**
	 * @param refundReplacementId the refundReplacementId to set
	 */
	public void setRefundReplacementId(String refundReplacementId) {
		this.refundReplacementId = refundReplacementId;
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

	/**
	 * @param customerMemoCode the customerMemoCode to set
	 */
	public void setCustomerMemoCode(String customerMemoCode) {
		this.customerMemoCode = customerMemoCode;
	}

	/**
	 * @param debitMemoId the debitMemoId to set
	 */
	public void setDebitMemoId(String debitMemoId) {
		this.debitMemoId = debitMemoId;
	}

	/**
	 * @param approvedBy the approvedBy to set
	 */
	public void setApprovedBy(String approvedBy) {
		this.approvedBy = approvedBy;
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
	 * @param debitMemo the debitMemo to set
	 */
	@BeanSubElement
	public void setDebitMemo(DebitMemoVO debitMemo) {
		this.debitMemo = debitMemo;
	}

	/**
	 * @param ticketIdText the ticketIdText to set
	 */
	public void setTicketIdText(String ticketIdText) {
		this.ticketIdText = ticketIdText;
	}

	/**
	 * @param filePathUrl the filePathUrl to set
	 */
	public void setFilePathUrl(String filePathUrl) {
		this.filePathUrl = filePathUrl;
	}

	/**
	 * @param productName the productName to set
	 */
	public void setProductName(String productName) {
		this.productName = productName;
	}

	/**
	 * @return the debitMemoCode
	 */
	@Column(name="debit_memo_code_txt", isReadOnly=true)
	public String getDebitMemoCode() {
		return debitMemoCode;
	}

	/**
	 * @param debitMemoCode the debitMemoCode to set
	 */
	public void setDebitMemoCode(String debitMemoCode) {
		this.debitMemoCode = debitMemoCode;
	}

}

