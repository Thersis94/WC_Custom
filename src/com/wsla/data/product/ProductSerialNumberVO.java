package com.wsla.data.product;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs 3.x
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.annotations.DataType;
import com.siliconmtn.annotations.Importable;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: ProductSerialNumberVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Holds a serial number data for a given product
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 15, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_product_serial")
public class ProductSerialNumberVO extends BeanDataVO {

	private static final long serialVersionUID = 4781013861998724814L;

	// Member Variables
	private String productSerialId;
	private String productId;
	private String serialNumber;
	private int validatedFlag;
	private Date createDate;
	private Date retailerDate;

	// Bean Sub-Elements
	private ProductVO product;
	private String warrantyName;
	private String warrantyId;

	public ProductSerialNumberVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ProductSerialNumberVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ProductSerialNumberVO(ResultSet rs) {
		super(rs);
	}


	/**
	 * @return the productSerialId
	 */
	@Column(name="product_serial_id", isPrimaryKey=true)
	public String getProductSerialId() {
		return productSerialId;
	}

	/**
	 * @return the productId
	 */
	@Column(name="product_id")
	public String getProductId() {
		return productId;
	}

	/**
	 * @return the serialNumber
	 */
	@Column(name="serial_no_txt")
	public String getSerialNumber() {
		return serialNumber;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	@Column(name="validated_flg")
	public int getValidatedFlag() {
		return validatedFlag;
	}

	@Column(name="retailer_dt")
	public Date getRetailerDate() {
		return retailerDate;
	}

	/**
	 * @return the product
	 */
	public ProductVO getProduct() {
		return product;
	}

	/**
	 * @param productSerialId the productSerialId to set
	 */
	public void setProductSerialId(String productSerialId) {
		this.productSerialId = productSerialId;
	}

	/**
	 * @param productId the productId to set
	 */
	public void setProductId(String productId) {
		this.productId = productId;
	}

	/**
	 * @param serialNumber the serialNumber to set
	 */
	@Importable(name="Serial Number", type=DataType.STRING)
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public void setValidatedFlag(int validatedFlag) {
		this.validatedFlag = validatedFlag;
	}

	public void setRetailerDate(Date retailerDate) {
		this.retailerDate = retailerDate;
	}

	/**
	 * @param product the product to set
	 */
	@BeanSubElement
	public void setProduct(ProductVO product) {
		this.product = product;
	}

	@Column(name="warranty_nm", isReadOnly=true)
	public String getWarrantyName() {
		return warrantyName;
	}

	public void setWarrantyName(String warrantyName) {
		this.warrantyName = warrantyName;
	}

	public String getWarrantyId() {
		return warrantyId;
	}

	public void setWarrantyId(String warrantyId) {
		this.warrantyId = warrantyId;
	}
}