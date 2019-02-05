package com.wsla.data.product;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: InventoryLedgerVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> value object used to record the manual manipulation of
 * inventory records
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Feb 1, 2019
 * @updates:
 ****************************************************************************/
@Table(name="wsla_inventory_ledger")
public class InventoryLedgerVO extends BeanDataVO {

	private static final long serialVersionUID = -2139690510086956507L;
	
	private String inventoryLedgerId;
	private String userId;
	private String itemMasterId;
	private int offsetNumber;
	private Date createDate;
	
	/**
	 * 
	 */
	public InventoryLedgerVO() {
		super();
	}

	/**
	 * @param req
	 */
	public InventoryLedgerVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public InventoryLedgerVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the inventoryLedgerId
	 */
	@Column(name="inventory_ledger_id", isPrimaryKey=true)
	public String getInventoryLedgerId() {
		return inventoryLedgerId;
	}

	/**
	 * @return the offsetNumber
	 */
	@Column(name="offset_no")
	public int getOffsetNumber() {
		return offsetNumber;
	}

	/**
	 * @return the itemMasterId
	 */
	@Column(name="item_master_id")
	public String getItemMasterId() {
		return itemMasterId;
	}

	/**
	 * @return the userId
	 */
	@Column(name="user_id")
	public String getUserId() {
		return userId;
	}
	
	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param itemMasterId the itemMasterId to set
	 */
	public void setItemMasterId(String itemMasterId) {
		this.itemMasterId = itemMasterId;
	}
	
	/**
	 * @param offsetNumber the offsetNumber to set
	 */
	public void setOffsetNumber(int offsetNumber) {
		this.offsetNumber = offsetNumber;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @param inventoryLedgerId the inventoryLedgerId to set
	 */
	public void setInventoryLedgerId(String inventoryLedgerId) {
		this.inventoryLedgerId = inventoryLedgerId;
	}

}
