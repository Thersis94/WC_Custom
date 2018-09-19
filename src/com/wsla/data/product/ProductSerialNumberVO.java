package com.wsla.data.product;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs 3.x
import com.siliconmtn.action.ActionRequest;
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
@Table(name="wsla_product_serial_number")
public class ProductSerialNumberVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4781013861998724814L;
	
	// Member Variables
	private String productSerialNumberId;
	private String productId;
	private String serialNumber;
	private String userId;
	private Date createDate;
	
	// Bean Sub-Elements
	private ProductVO product;

	/**
	 * 
	 */
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
	 * @return the productSerialNumberId
	 */
	@Column(name="product_serial_number_id", isPrimaryKey=true)
	public String getProductSerialNumberId() {
		return productSerialNumberId;
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
	 * @return the product
	 */
	public ProductVO getProduct() {
		return product;
	}

	/**
	 * @param productSerialNumberId the productSerialNumberId to set
	 */
	public void setProductSerialNumberId(String productSerialNumberId) {
		this.productSerialNumberId = productSerialNumberId;
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
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param product the product to set
	 */
	@BeanSubElement
	public void setProduct(ProductVO product) {
		this.product = product;
	}
}

