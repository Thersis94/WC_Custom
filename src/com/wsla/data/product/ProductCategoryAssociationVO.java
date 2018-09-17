package com.wsla.data.product;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: ProductCategoryAssociation.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value Object that associates the product category to a 
 * specific product
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 15, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_product_category_xr")
public class ProductCategoryAssociationVO extends ProductCategoryVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2867788858242677240L;
	
	// Member Variables
	private String categoryAssociationId;
	private String productId;

	/**
	 * 
	 */
	public ProductCategoryAssociationVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ProductCategoryAssociationVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ProductCategoryAssociationVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the categoryAssociationId
	 */
	@Column(name="category_xr_id", isPrimaryKey=true)
	public String getCategoryAssociationId() {
		return categoryAssociationId;
	}

	/**
	 * @return the productId
	 */
	@Column(name="product_id")
	public String getProductId() {
		return productId;
	}

	/**
	 * @param categoryAssociationId the categoryAssociationId to set
	 */
	public void setCategoryAssociationId(String categoryAssociationId) {
		this.categoryAssociationId = categoryAssociationId;
	}

	/**
	 * @param productId the productId to set
	 */
	public void setProductId(String productId) {
		this.productId = productId;
	}

}

