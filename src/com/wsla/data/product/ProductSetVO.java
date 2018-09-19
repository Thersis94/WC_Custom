package com.wsla.data.product;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs 3.x
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: ProductSetVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object to hold data for the parts that make up a set
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 15, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_product_set")
public class ProductSetVO extends ProductVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 816517778122169454L;
	
	// Member Variables
	private String productSetId;
	private String partId;
	private String setId;
	private int quantity;
	
	/**
	 * 
	 */
	public ProductSetVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ProductSetVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ProductSetVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the productSetId
	 */
	@Column(name="set_xr_id", isPrimaryKey=true)
	public String getProductSetId() {
		return productSetId;
	}

	/**
	 * @return the partId
	 */
	@Column(name="product_id")
	public String getPartId() {
		return partId;
	}

	/**
	 * @return the setId
	 */
	@Column(name="set_id")
	public String getSetId() {
		return setId;
	}

	/**
	 * @return the quantity
	 */
	@Column(name="qnty_no")
	public int getQuantity() {
		return quantity;
	}

	/**
	 * @param productSetId the productSetId to set
	 */
	public void setProductSetId(String productSetId) {
		this.productSetId = productSetId;
	}

	/**
	 * @param partId the partId to set
	 */
	public void setPartId(String partId) {
		this.partId = partId;
	}

	/**
	 * @param setId the setId to set
	 */
	public void setSetId(String setId) {
		this.setId = setId;
	}

	/**
	 * @param quantity the quantity to set
	 */
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

}

