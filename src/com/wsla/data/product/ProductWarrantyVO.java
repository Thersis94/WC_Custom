package com.wsla.data.product;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: ProductWarrantyVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object to assign a warranty to a given 
 * product serial number
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 15, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_product_warranty")
public class ProductWarrantyVO extends WarrantyVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4567698594237550575L;

	// Member Variables
	private String productWarrantyId;
	private String productSerialNumberId;
	private Date expirationDate;

	// Bean Sub-elements
	private ProductSerialNumberVO productSerialNumber = null;
	
	/**
	 * 
	 */
	public ProductWarrantyVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ProductWarrantyVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ProductWarrantyVO(ResultSet rs) {
		super(rs);
	}

	public ProductWarrantyVO(String productSerialId, String warrantyId, Date expDate) {
		this();
		setProductSerialNumberId(productSerialId);
		setWarrantyId(warrantyId);
		setExpirationDate(expDate);
	}

	/**
	 * @return the productWarrantyId
	 */
	@Column(name="product_warranty_id", isPrimaryKey=true)
	public String getProductWarrantyId() {
		return productWarrantyId;
	}

	/*
	 * (non-Javadoc)
	 * @see com.wsla.data.product.WarrantyVO#getWarrantyId()
	 */
	@Override
	@Column(name="warranty_id")
	public String getWarrantyId() {
		return super.getWarrantyId();
	}

	/**
	 * @return the productSerialNumberId
	 */
	@Column(name="product_serial_id")
	public String getProductSerialNumberId() {
		return productSerialNumberId;
	}

	/**
	 * @return the expirationDate
	 */
	@Column(name="expiration_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getExpirationDate() {
		return expirationDate;
	}

	/**
	 * @return the productSerialNumber
	 */
	public ProductSerialNumberVO getProductSerialNumber() {
		return productSerialNumber;
	}

	/**
	 * @param productWarrantyId the productWarrantyId to set
	 */
	public void setProductWarrantyId(String productWarrantyId) {
		this.productWarrantyId = productWarrantyId;
	}

	/**
	 * @param productSerialNumberId the productSerialNumberId to set
	 */
	public void setProductSerialNumberId(String productSerialNumberId) {
		this.productSerialNumberId = productSerialNumberId;
	}

	/**
	 * @param expirationDate the expirationDate to set
	 */
	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	/**
	 * @param productSerialNumber the productSerialNumber to set
	 */
	@BeanSubElement
	public void setProductSerialNumber(ProductSerialNumberVO productSerialNumber) {
		this.productSerialNumber = productSerialNumber;
	}

}

