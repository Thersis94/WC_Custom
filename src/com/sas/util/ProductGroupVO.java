package com.sas.util;

import java.io.Serializable;

import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ProductGroupVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Nov 30, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ProductGroupVO implements Serializable, Comparable<ProductGroupVO> {
	private String id = null;
	private String name = null;
	private String desc = null;
	private String imageUrl = null;
	private String category = null;
	private String thumbnail = null;
	private String productUrl = null;
	private double priceRangeLow = 0;
	private double priceRangeHigh = 0;
	private String rating = null;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public ProductGroupVO() {
		
	}
	
	public ProductGroupVO(String id) {
		this.id = id;
	}
	
	/**
	 * Returns a string representation of the price.  If the high and low price
	 * are the same for the group, a single price is returned.  Otherwise, the 
	 * range is provided
	 * @return
	 */
	public String getPriceRange() {
		if (priceRangeLow == priceRangeHigh) return "$" + priceRangeLow;
		else return "$" + priceRangeLow + " - " + "$" + priceRangeHigh;
	}
	
	public String toString() {
		return id;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		ProductGroupVO vo = (ProductGroupVO)o;
		if (vo == null) {
			vo = new ProductGroupVO();
		}
		
		return StringUtil.checkVal(id).equals(vo.getId());
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(ProductGroupVO o) {
		ProductGroupVO vo = (ProductGroupVO)o;
		if (vo == null) {
			
			vo = new ProductGroupVO();
		}
		
		
		return StringUtil.checkVal(id).compareTo(vo.getId());
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return Convert.formatInteger(id);
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the desc
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * @param desc the desc to set
	 */
	public void setDesc(String desc) {
		this.desc = desc;
	}

	/**
	 * @return the imageUrl
	 */
	public String getImageUrl() {
		return imageUrl;
	}

	/**
	 * @param imageUrl the imageUrl to set
	 */
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	/**
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * @param category the category to set
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * @return the thumbnail
	 */
	public String getThumbnail() {
		return thumbnail;
	}

	/**
	 * @param thumbnail the thumbnail to set
	 */
	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

	/**
	 * @return the productUrl
	 */
	public String getProductUrl() {
		return productUrl;
	}

	/**
	 * @param productUrl the productUrl to set
	 */
	public void setProductUrl(String productUrl) {
		this.productUrl = productUrl;
	}

	/**
	 * @return the rating
	 */
	public String getRating() {
		return rating;
	}

	/**
	 * @param rating the rating to set
	 */
	public void setRating(String rating) {
		this.rating = rating;
	}

	/**
	 * @return the priceRangeLow
	 */
	public double getPriceRangeLow() {
		return priceRangeLow;
	}

	/**
	 * @param priceRangeLow the priceRangeLow to set
	 */
	public void setPriceRangeLow(double priceRangeLow) {
		this.priceRangeLow = priceRangeLow;
	}

	/**
	 * @return the priceRangeHigh
	 */
	public double getPriceRangeHigh() {
		return priceRangeHigh;
	}

	/**
	 * @param priceRangeHigh the priceRangeHigh to set
	 */
	public void setPriceRangeHigh(double priceRangeHigh) {
		this.priceRangeHigh = priceRangeHigh;
	}

}
