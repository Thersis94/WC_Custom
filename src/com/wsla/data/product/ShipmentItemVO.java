package com.wsla.data.product;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: ShipmentItemVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the items added to the shipment
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 17, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_shipment_item")
public class ShipmentItemVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4075476260561903159L;
	
	// Member Variables
	private String shipmentItemId;
	private String itemMasterId;
	private String shipmentId;
	private String comments;
	private Date createDate;
	private Date updateDate;

	/**
	 * 
	 */
	public ShipmentItemVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ShipmentItemVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ShipmentItemVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the shipmentItemId
	 */
	@Column(name="shipment_item_id", isPrimaryKey=true)
	public String getShipmentItemId() {
		return shipmentItemId;
	}

	/**
	 * @return the itemMasterId
	 */
	@Column(name="item_master_id")
	public String getItemMasterId() {
		return itemMasterId;
	}

	/**
	 * @return the shipmentId
	 */
	@Column(name="shipment_id")
	public String getShipmentId() {
		return shipmentId;
	}

	/**
	 * @return the comments
	 */
	@Column(name="comments_txt")
	public String getComments() {
		return comments;
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
	 * @param shipmentItemId the shipmentItemId to set
	 */
	public void setShipmentItemId(String shipmentItemId) {
		this.shipmentItemId = shipmentItemId;
	}

	/**
	 * @param itemMasterId the itemMasterId to set
	 */
	public void setItemMasterId(String itemMasterId) {
		this.itemMasterId = itemMasterId;
	}

	/**
	 * @param shipmentId the shipmentId to set
	 */
	public void setShipmentId(String shipmentId) {
		this.shipmentId = shipmentId;
	}

	/**
	 * @param comments the comments to set
	 */
	public void setComments(String comments) {
		this.comments = comments;
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

