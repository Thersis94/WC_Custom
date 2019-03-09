package com.wsla.data.product;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

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

	private static final long serialVersionUID = -9010122525350010581L;

	// Member Variables
	private String productCategoryId;
	private String parentId;
	private String categoryCode;
	private int activeFlag;
	private String groupCode;
	private Date createDate;

	private String parentCode;

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
	 * @return the productCategoryId
	 */
	@Column(name="product_category_id", isPrimaryKey=true)
	public String getProductCategoryId() {
		return productCategoryId;
	}

	/**
	 * @return the parentId
	 */
	@Column(name="parent_id")
	public String getParentId() {
		return parentId;
	}

	/**
	 * @return the categoryName
	 */
	@Column(name="category_cd")
	public String getCategoryCode() {
		return categoryCode;
	}

	@Column(name="group_cd")
	public String getGroupCode() {
		return groupCode;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	@Column(name="parent_cd")
	public String getParentCode() {
		return parentCode;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @param productCategorCode the productCategoryId to set
	 */
	public void setProductCategoryId(String productCategoryId) {
		this.productCategoryId = productCategoryId;
	}

	/**
	 * @param parentId the parentId to set
	 */
	public void setParentId(String parentId) {
		this.parentId = StringUtil.checkVal(parentId, null);
	}

	/**
	 * @param categoryName the categoryCode to set
	 */
	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public void setGroupCode(String groupCode) {
		this.groupCode = groupCode;
	}

	public void setParentCode(String parentCode) {
		this.parentCode = parentCode;
	}
}