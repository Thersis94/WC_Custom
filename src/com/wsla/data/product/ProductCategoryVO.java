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
 * <b>Title</b>: ProductCategoryVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value Object holding information for the product 
 * master categories.  This data is also utilized to identify which service centers
 * manage which products
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 15, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_product_category")
public class ProductCategoryVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9010122525350010581L;
	
	// Member Variables
	private String productCategorCode;
	private String parentId;
	private String categoryName;
	private Date createDate;

	/**
	 * 
	 */
	public ProductCategoryVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ProductCategoryVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ProductCategoryVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the productCategorCode
	 */
	@Column(name="product_category_cd", isPrimaryKey=true)
	public String getProductCategorCode() {
		return productCategorCode;
	}

	/**
	 * @return the parentId
	 */
	@Column(name="parentId")
	public String getParentId() {
		return parentId;
	}

	/**
	 * @return the categoryName
	 */
	@Column(name="category_nm")
	public String getCategoryName() {
		return categoryName;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param productCategorCode the productCategorCode to set
	 */
	public void setProductCategorCode(String productCategorCode) {
		this.productCategorCode = productCategorCode;
	}

	/**
	 * @param parentId the parentId to set
	 */
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	/**
	 * @param categoryName the categoryName to set
	 */
	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}

