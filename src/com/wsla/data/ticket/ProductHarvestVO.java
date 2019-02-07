package com.wsla.data.ticket;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.wsla.data.product.ProductSerialNumberVO;

/****************************************************************************
 * <p><b>Title:</b> ProductHarvestVO.java</p>
 * <p><b>Description:</b> VO form of product_harvest table.  Used on Harvesting screens.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2018, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Oct 30, 2018
 * <b>Changes:</b>
 ****************************************************************************/
@Table(name="wsla_product_harvest")
public class ProductHarvestVO extends ProductSerialNumberVO {

	private static final long serialVersionUID = -5010853405721828175L;

	private String productHarvestId;
	private OutcomeCode outcomeCode;
	private int quantity;
	private String note;
	private String locationId;
	
	// Helper variables
	private int productSetQuantity;

	/*
	 * Possible outcomes from parts harvesting.
	 */
	public enum OutcomeCode {
		RECLAIMED, DEFECTIVE, DAMAGED, MISSING, OTHER
	}

	public ProductHarvestVO() {
		super();
	}

	public ProductHarvestVO(ActionRequest req) {
		super(req);
	}

	@Column(name="product_harvest_id", isPrimaryKey=true)
	public String getProductHarvestId() {
		return productHarvestId;
	}

	@Column(name="outcome_cd")
	public OutcomeCode getOutcomeCode() {
		return outcomeCode;
	}

	@Column(name="qnty_no")
	public int getQuantity() {
		return quantity;
	}

	@Column(name="note_txt")
	public String getNote() {
		return note;
	}

	// This isn't part of the SQL table yet, but we pass it in on the build call for use in locn_item_master updates
	public String getLocationId() {
		return locationId;
	}

	public void setProductHarvestId(String productHarvestId) {
		this.productHarvestId = productHarvestId;
	}

	public void setOutcomeCode(OutcomeCode outcomeCode) {
		this.outcomeCode = outcomeCode;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	/**
	 * @return the productSetQuantity
	 */
	@Column(name="set_qnty_no", isReadOnly=true)
	public int getProductSetQuantity() {
		return productSetQuantity;
	}

	/**
	 * @param productSetQuantity the productSetQuantity to set
	 */
	public void setProductSetQuantity(int productSetQuantity) {
		this.productSetQuantity = productSetQuantity;
	}
}