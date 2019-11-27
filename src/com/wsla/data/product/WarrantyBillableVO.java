package com.wsla.data.product;

// JDK 1.8.x
import java.sql.ResultSet;

//SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

// WSLA Libs
import com.wsla.data.ticket.BillableActivityVO;

/****************************************************************************
 * <b>Title</b>: WarrantyBillableVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for storing information about the billable rates
 * for a given warranty and activity
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Nov 7, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_warranty_billable_xr")
public class WarrantyBillableVO extends BillableActivityVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7713485931215347372L;

	// Member Variables
	private String warrantyBillableId;
	private String warrantyId;
	private double cost;
	private double invoiceAmount;
	
	/**
	 * 
	 */
	public WarrantyBillableVO() {
		super();
	}

	/**
	 * @param req
	 */
	public WarrantyBillableVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public WarrantyBillableVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the warrantyBillableId
	 */
	@Column(name="warranty_billable_id", isPrimaryKey=true)
	public String getWarrantyBillableId() {
		return warrantyBillableId;
	}

	/**
	 * @return the warrantyId
	 */
	@Column(name="warranty_id")
	public String getWarrantyId() {
		return warrantyId;
	}

	/**
	 * @return the cost
	 */
	@Column(name="cost_no")
	public double getCost() {
		return cost;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.wsla.data.ticket.BillableActivityVO#getBillableActivityCode()
	 */
	@Column(name="billable_activity_cd")
	@Override
	public String getBillableActivityCode() {
		return super.getBillableActivityCode();
	}

	/**
	 * @param warrantyBillableId the warrantyBillableId to set
	 */
	public void setWarrantyBillableId(String warrantyBillableId) {
		this.warrantyBillableId = warrantyBillableId;
	}

	/**
	 * @param warrantyId the warrantyId to set
	 */
	public void setWarrantyId(String warrantyId) {
		this.warrantyId = warrantyId;
	}

	/**
	 * @param cost the cost to set
	 */
	public void setCost(double cost) {
		this.cost = cost;
	}

	/**
	 * @return the invoiceAmount
	 */
	@Column(name="invoice_amount_no")
	public double getInvoiceAmount() {
		return invoiceAmount;
	}

	/**
	 * @param invoiceAmount the invoiceAmount to set
	 */
	public void setInvoiceAmount(double invoiceAmount) {
		this.invoiceAmount = invoiceAmount;
	}

}
