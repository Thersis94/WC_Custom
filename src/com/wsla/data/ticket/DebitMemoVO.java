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

// WC Libs
import com.wsla.data.provider.ProviderVO;

/****************************************************************************
 * <b>Title</b>: DebitMemoVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the Debit Memo data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 8, 2019
 * @updates:
 ****************************************************************************/
@Table(name="wsla_debit_memo")
public class DebitMemoVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3300811711353420674L;

	// String Member Variables
	private String debitMemoId;
	private String customerMemoCode;
	private String oemId;
	private String retailId;
	private String approvedBy;
	private String transferNumber;
	private String filePathUrl;
	private String retailerName;
	private String oemName;

	// Numeric Members
	private double transferAmount;
	private double totalCreditMemoAmount;
	private long totalCreditMemos;

	// Date Members
	private Date approvalDate;
	private Date transferDate;
	private Date createDate;
	private Date updateDate;

	// Sub Beans
	private List<CreditMemoVO> creditMemos = new ArrayList<>();
	private ProviderVO oem;
	private ProviderVO retailer;

	/**
	 * 
	 */
	public DebitMemoVO() {
		super();
	}

	/**
	 * @param req
	 */
	public DebitMemoVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public DebitMemoVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * Checks the data to see if the transfer data was added so the transfer 
	 * date can be assigned
	 */
	public void assignTransferDate() {
		if (transferDate == null && (! StringUtil.isEmpty(transferNumber) || transferAmount > 0)) {
			transferDate = new Date();
		}
	}

	/**
	 * Set the approval date if empty and approved by has been passed
	 */
	public void assignApprovalDate() {

		if (getApprovalDate() == null && ! StringUtil.isEmpty(getApprovedBy())) {
			setApprovalDate(new Date());
		}
	}

	/**
	 * @return the debitMemoId
	 */
	@Column(name="debit_memo_id", isPrimaryKey=true)
	public String getDebitMemoId() {
		return debitMemoId;
	}

	/**
	 * @return the customerMemoCode
	 */
	@Column(name="customer_memo_cd")
	public String getCustomerMemoCode() {
		return customerMemoCode;
	}

	/**
	 * @return the oemId
	 */
	@Column(name="oem_id")
	public String getOemId() {
		return oemId;
	}

	/**
	 * @return the retailId
	 */
	@Column(name="retail_id")
	public String getRetailId() {
		return retailId;
	}

	/**
	 * @return the approvedBy
	 */
	@Column(name="approved_by_txt")
	public String getApprovedBy() {
		return approvedBy;
	}

	/**
	 * @return the transferNumber
	 */
	@Column(name="transfer_no_txt")
	public String getTransferNumber() {
		return transferNumber;
	}

	/**
	 * @return the transferAmount
	 */
	@Column(name="amount_no")
	public double getTransferAmount() {
		return transferAmount;
	}

	/**
	 * @return the approvalDate
	 */
	@Column(name="approval_dt")
	public Date getApprovalDate() {
		return approvalDate;
	}

	/**
	 * @return the transferDate
	 */
	@Column(name="transfer_dt")
	public Date getTransferDate() {
		return transferDate;
	}

	/**
	 * @return the totalCreditMemoAmount
	 */
	@Column(name="total_credit_memo", isReadOnly=true)
	public double getTotalCreditMemoAmount() {
		if (Double.valueOf(totalCreditMemoAmount) == 0 && !creditMemos.isEmpty()) {
			for (CreditMemoVO cm : creditMemos) {
				totalCreditMemoAmount += cm.getRefundAmount();
			}
		}

		return totalCreditMemoAmount;
	}

	/**
	 * @return the filePathUrl
	 */
	@Column(name="document_path_url")
	public String getFilePathUrl() {
		return filePathUrl;
	}

	/**
	 * @return the retailerName
	 */
	@Column(name="retailer_nm", isReadOnly=true)
	public String getRetailerName() {
		return retailerName;
	}

	/**
	 * @return the oemName
	 */
	@Column(name="oem_nm", isReadOnly=true)
	public String getOemName() {
		return oemName;
	}

	/**
	 * @return the totalCreditMemos
	 */
	@Column(name="credit_memo_no")
	public long getTotalCreditMemos() {
		return totalCreditMemos;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the oem
	 */
	public ProviderVO getOem() {
		return oem;
	}

	/**
	 * @return the retailer
	 */
	public ProviderVO getRetailer() {
		return retailer;
	}

	/**
	 * @param debitMemoId the debitMemoId to set
	 */
	public void setDebitMemoId(String debitMemoId) {
		this.debitMemoId = debitMemoId;
	}

	/**
	 * @param customerMemoCode the customerMemoCode to set
	 */
	public void setCustomerMemoCode(String customerMemoCode) {
		this.customerMemoCode = customerMemoCode;
	}

	/**
	 * @param oemId the oemId to set
	 */
	public void setOemId(String oemId) {
		this.oemId = oemId;
	}

	/**
	 * @param retailId the retailId to set
	 */
	public void setRetailId(String retailId) {
		this.retailId = retailId;
	}

	/**
	 * @param approvedBy the approvedBy to set
	 */
	public void setApprovedBy(String approvedBy) {
		this.approvedBy = approvedBy;
	}

	/**
	 * @param transferNumber the transferNumber to set
	 */
	public void setTransferNumber(String transferNumber) {
		this.transferNumber = transferNumber;
	}

	/**
	 * @param transferAmount the transferAmount to set
	 */
	public void setTransferAmount(double transferAmount) {
		this.transferAmount = transferAmount;
	}

	/**
	 * @param approvalDate the approvalDate to set
	 */
	public void setApprovalDate(Date approvalDate) {
		this.approvalDate = approvalDate;
	}

	/**
	 * @param transferDate the transferDate to set
	 */
	public void setTransferDate(Date transferDate) {
		this.transferDate = transferDate;
	}

	/**
	 * @return the creditMemos
	 */
	public List<CreditMemoVO> getCreditMemos() {
		return creditMemos;
	}

	/**
	 * @param creditMemos the creditMemos to set
	 */
	public void setCreditMemos(List<CreditMemoVO> creditMemos) {
		this.creditMemos = creditMemos;
	}

	/**
	 * 
	 * @param creditMemo
	 */
	@BeanSubElement
	public void addCreditMemo(CreditMemoVO creditMemo) {
		if (creditMemo != null) creditMemos.add(creditMemo);
	}

	/**
	 * @param totalCreditMemoAmount the totalCreditMemoAmount to set
	 */
	public void setTotalCreditMemoAmount(double totalCreditMemoAmount) {
		this.totalCreditMemoAmount = totalCreditMemoAmount;
	}

	/**
	 * @param oem the oem to set
	 */
	@BeanSubElement
	public void setOem(ProviderVO oem) {
		this.oem = oem;
	}

	/**
	 * @param retailer the retailer to set
	 */
	@BeanSubElement
	public void setRetailer(ProviderVO retailer) {
		this.retailer = retailer;
	}

	/**
	 * @param filePathUrl the filePathUrl to set
	 */
	public void setFilePathUrl(String filePathUrl) {
		this.filePathUrl = filePathUrl;
	}

	/**
	 * @param retailerName the retailerName to set
	 */
	public void setRetailerName(String retailerName) {
		this.retailerName = retailerName;
	}

	/**
	 * @param oemName the oemName to set
	 */
	public void setOemName(String oemName) {
		this.oemName = oemName;
	}

	/**
	 * @param totalCreditMemos the totalCreditMemos to set
	 */
	public void setTotalCreditMemos(long totalCreditMemos) {
		this.totalCreditMemos = totalCreditMemos;
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
}

