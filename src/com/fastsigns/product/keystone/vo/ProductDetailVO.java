package com.fastsigns.product.keystone.vo;


/****************************************************************************
 * <b>Title</b>: ProductDetailVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 9, 2012
 ****************************************************************************/
public class ProductDetailVO extends KeystoneProductVO implements Cloneable{
	private static final long serialVersionUID = 13254321L;
	private String dimensions = null;
	private String webId = null;
	private String franchiseAliasId = null;

	public ProductDetailVO() {
	}

	public double getUnit_price() {
		return super.getMsrpCostNo();
	}

	public void setUnit_price(double unit_price) {
		super.setMsrpCostNo(unit_price);
	}

	/** 
	 * dimensions are "the desired dimensions" of the sign, which get displayed in the shopping cart.
	 * @return
	 */
	public String getDimensions() {
		return dimensions;
	}

	public void setDimensions(String dimensions) {
		this.dimensions = dimensions;
	}

	public String getFranchiseAliasId() {
		return franchiseAliasId;
	}

	public void setFranchiseAliasId(String franchiseAliasId) {
		this.franchiseAliasId = franchiseAliasId;
	}

	public String getWebId() {
		return webId;
	}

	public void setWebId(String webId) {
		this.webId = webId;
	}
	
	public ProductDetailVO clone() throws CloneNotSupportedException {
		return (ProductDetailVO) super.clone();
	}
}
