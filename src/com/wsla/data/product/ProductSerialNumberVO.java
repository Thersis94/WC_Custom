package com.wsla.data.product;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs 3.x
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.annotations.DataType;
import com.siliconmtn.annotations.Importable;
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
public class ProductSerialNumberVO extends ProductVO {

	private static final long serialVersionUID = 4781013861998724814L;

	// Member Variables
	private String productSerialId;
	private String serialNumber;
	private Date retailerDate;
	private double retailerCost;
	private int disposeFlag;

	// Bean Sub-Elements
	private ProductVO product;
	private String warrantyName;
	
	// Helper members
	private String warrantyId;
	private String productWarrantyId;
	
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
	@Override
	public String getProductId() {
		return super.getProductId();
	}

	/**
	 * @return the serialNumber
	 */
	@Column(name="serial_no_txt")
	public String getSerialNumber() {
		return serialNumber;
	}

	/**
	 * @return the retailerCost
	 */
	@Column(name="retailer_cost_no")
	public double getRetailerCost() {
		return retailerCost;
	}

	/**
	 * @return the createDate
	 */
	@Override
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return super.getCreateDate();
	}

	/**
	 * @return the updateDate
	 */
	@Override
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return super.getUpdateDate();
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
	 * @param serialNumber the serialNumber to set
	 */
	@Importable(name="Serial Number", type=DataType.STRING)
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
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

	/**
	 * 
	 * @return
	 */
	@Column(name="warranty_id", isReadOnly=true)
	public String getWarrantyId() {
		return warrantyId;
	}

	public void setWarrantyId(String warrantyId) {
		this.warrantyId = warrantyId;
	}

	/**
	 * @return the productWarrantyId
	 */
	@Column(name="product_warranty_id", isReadOnly=true)
	public String getProductWarrantyId() {
		return productWarrantyId;
	}

	/**
	 * @param productWarrantyId the productWarrantyId to set
	 */
	public void setProductWarrantyId(String productWarrantyId) {
		this.productWarrantyId = productWarrantyId;
	}

	/**
	 * @param retailerCost the retailerCost to set
	 */
	public void setRetailerCost(double retailerCost) {
		this.retailerCost = retailerCost;
	}

	/**
	 * @return the disposeFlag
	 */
	@Column(name="dispose_flg")
	public int getDisposeFlag() {
		return disposeFlag;
	}

	/**
	 * @param disposeFlag the disposeFlag to set
	 */
	public void setDisposeFlag(int disposeFlag) {
		this.disposeFlag = disposeFlag;
	}
}