package com.wsla.data.product;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
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
public class ProductWarrantyVO extends BeanDataVO {

	private static final long serialVersionUID = -4567698594237550575L;

	// Member Variables
	private String productWarrantyId;
	private String productSerialId;
	private Date expirationDate;
	private String warrantyId;
	private WarrantyVO warranty;
	private Date createDate;
	private Date updateDate;


	/**
	 * 
	 */
	public ProductWarrantyVO() {
		super();
		warranty = new WarrantyVO();
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
		setProductSerialId(productSerialId);
		warranty.setWarrantyId(warrantyId);
		setExpirationDate(expDate);
	}

	/**
	 * @return the productWarrantyId
	 */
	@Column(name="product_warranty_id", isPrimaryKey=true)
	public String getProductWarrantyId() {
		return productWarrantyId;
	}

	@Column(name="warranty_id")
	public String getWarrantyId() {
		return warrantyId;
	}

	/**
	 * @return the productSerialId
	 */
	@Column(name="product_serial_id")
	public String getProductSerialId() {
		return productSerialId;
	}

	/**
	 * @return the expirationDate
	 */
	@Column(name="expiration_dt")
	public Date getExpirationDate() {
		return expirationDate;
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
	 * @param productWarrantyId the productWarrantyId to set
	 */
	public void setProductWarrantyId(String productWarrantyId) {
		this.productWarrantyId = productWarrantyId;
	}

	public void setWarrantyId(String warrantyId) {
		this.warrantyId = warrantyId;
	}

	/**
	 * @param productSerialNumberId the productSerialId to set
	 */
	public void setProductSerialId(String productSerialId) {
		this.productSerialId = productSerialId;
	}

	/**
	 * @param expirationDate the expirationDate to set
	 */
	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
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

	public WarrantyVO getWarranty() {
		return warranty;
	}

	@BeanSubElement
	public void setWarranty(WarrantyVO warranty) {
		this.warranty = warranty;
	}
}