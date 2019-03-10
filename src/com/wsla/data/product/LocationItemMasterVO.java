package com.wsla.data.product;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs 3.x
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.wsla.data.provider.ProviderLocationVO;

/****************************************************************************
 * <b>Title</b>: LocationItemMaster.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object holding the product on hand and par values
 * for a given product at a given location
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 15, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_location_item_master")
public class LocationItemMasterVO extends ProductVO {

	private static final long serialVersionUID = 1757453128482996982L;

	// Member Variables
	private String itemMasterId;
	private String locationId;
	private int quantityOnHand;
	private int parValue;

	// Bean Sub-Elements
	ProviderLocationVO location;

	/**
	 * 
	 */
	public LocationItemMasterVO() {
		super();
	}

	/**
	 * @param req
	 */
	public LocationItemMasterVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public LocationItemMasterVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the itemMasterId
	 */
	@Column(name="item_master_id", isPrimaryKey=true)
	public String getItemMasterId() {
		return itemMasterId;
	}

	/**
	 * Override this field so there is only one PK on this bean
	 */
	@Column(name="product_id")
	@Override
	public String getProductId() {
		return super.getProductId();
	}

	/**
	 * @return the locationId
	 */
	@Column(name="location_id")
	public String getLocationId() {
		return locationId;
	}

	/**
	 * @return the quantityOnHand
	 */
	@Column(name="actual_qnty_no")
	public int getQuantityOnHand() {
		return quantityOnHand;
	}

	/**
	 * @return the parValue
	 */
	@Column(name="desired_qnty_no")
	public int getParValue() {
		return parValue;
	}

	/**
	 * @return the location
	 */
	public ProviderLocationVO getLocation() {
		return location;
	}

	/**
	 * @param itemMasterId the itemMasterId to set
	 */
	public void setItemMasterId(String itemMasterId) {
		this.itemMasterId = itemMasterId;
	}

	/**
	 * @param locationId the locationId to set
	 */
	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	/**
	 * @param quantityOnHand the quantityOnHand to set
	 */
	public void setQuantityOnHand(int quantityOnHand) {
		this.quantityOnHand = quantityOnHand;
	}

	/**
	 * @param parValue the parValue to set
	 */
	public void setParValue(int parValue) {
		this.parValue = parValue;
	}

	/**
	 * @param location the location to set
	 */
	@BeanSubElement
	public void setLocation(ProviderLocationVO location) {
		this.location = location;
	}
}