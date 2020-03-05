package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: BillableActivityVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the billable codes
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Nov 7, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_billable_activity")
public class BillableActivityVO extends BeanDataVO {
	
	/**
	 * 
	 */
	public enum BillableTypeCode {
		STATUS("Status Code Activity"), 
		ACTIVITY("WSLA User Activity"),
		REPAIR_TYPE("Unit Repair Type"),
		REPAIR("Unit Repair Activity");
		
		protected String typeName;
		public String getTypeName() { return typeName; }
		private BillableTypeCode(String typeName) { this.typeName = typeName; }
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 4882894206859075793L;

	// Member Variables
	private String billableActivityCode;
	private String activityName;
	private BillableTypeCode billableTypeCode;
	private int activeFlag;
	private String parentId;
	private Date createDate;
	private double defaultCost;
	private double defaultInvoiceAmount;
	
	/**
	 * 
	 */
	public BillableActivityVO() {
		super();
	}

	/**
	 * @param req
	 */
	public BillableActivityVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public BillableActivityVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the billableActivityCode
	 */
	@Column(name="billable_activity_cd", isPrimaryKey=true)
	public String getBillableActivityCode() {
		return billableActivityCode;
	}

	/**
	 * @return the activityName
	 */
	@Column(name="activity_nm")
	public String getActivityName() {
		return activityName;
	}

	/**
	 * @return the billableTypeCode
	 */
	@Column(name="billable_type_cd")
	public BillableTypeCode getBillableTypeCode() {
		return billableTypeCode;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the parentId
	 */
	@Column(name="parent_id")
	public String getParentId() {
		return parentId;
	}

	/**
	 * @return the defaultCost
	 */
	@Column(name="default_cost_no")
	public double getDefaultCost() {
		return defaultCost;
	}

	/**
	 * @return the defaultInvoiceAmount
	 */
	@Column(name="default_invoice_amt_no")
	public double getDefaultInvoiceAmount() {
		return defaultInvoiceAmount;
	}

	/**
	 * @param parentId the parentId to set
	 */
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	/**
	 * @param billableActivityCode the billableActivityCode to set
	 */
	public void setBillableActivityCode(String billableActivityCode) {
		this.billableActivityCode = billableActivityCode;
	}

	/**
	 * @param activityName the activityName to set
	 */
	public void setActivityName(String activityName) {
		this.activityName = activityName;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param billableTypeCode the billableTypeCode to set
	 */
	public void setBillableTypeCode(BillableTypeCode billableTypeCode) {
		this.billableTypeCode = billableTypeCode;
	}

	/**
	 * @param defaultCost the defaultCost to set
	 */
	public void setDefaultCost(double defaultCost) {
		this.defaultCost = defaultCost;
	}

	/**
	 * @param defaultInvoiceAmount the defaultInvoiceAmount to set
	 */
	public void setDefaultInvoiceAmount(double defaultInvoiceAmount) {
		this.defaultInvoiceAmount = defaultInvoiceAmount;
	}

}

