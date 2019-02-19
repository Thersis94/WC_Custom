package com.wsla.data.admin;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: InvoiceTypeCasVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Data bean for the provider - invoice type cross ref table
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 24, 2019
 * @updates:
 ****************************************************************************/
@Table(name="wsla_invoice_cas_xr")
public class InvoiceTypeCasVO extends InvoiceTypeVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 367086135168901632L;

	// Members
	private String invoiceCasId;
	private String providerId;
	private double amount;
	
	/**
	 * 
	 */
	public InvoiceTypeCasVO() {
		super();
	}

	/**
	 * @param req
	 */
	public InvoiceTypeCasVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public InvoiceTypeCasVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the invoiceCasId
	 */
	@Column(name="invoice_cas_xr_id", isPrimaryKey=true)
	public String getInvoiceCasId() {
		return invoiceCasId;
	}

	/**
	 * @return the providerId
	 */
	@Column(name="provider_id")
	public String getProviderId() {
		return providerId;
	}

	/**
	 * @return the amount
	 */
	@Column(name="amount_no")
	public double getAmount() {
		return amount;
	}

	/**
	 * @param invoiceCasId the invoiceCasId to set
	 */
	public void setInvoiceCasId(String invoiceCasId) {
		this.invoiceCasId = invoiceCasId;
	}

	/**
	 * @param providerId the providerId to set
	 */
	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	/**
	 * @param amount the amount to set
	 */
	public void setAmount(double amount) {
		this.amount = amount;
	}
}

