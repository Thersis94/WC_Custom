/**
 * 
 */
package com.fastsigns.product.keystone.vo;

import java.io.Serializable;

/****************************************************************************
 * <b>Title</b>: SizeVO.java<p/>
 * <b>Description: Available Sizes for a product.  Used at the ProductDetail level.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 19, 2012
 ****************************************************************************/
public class SizeVO implements Serializable, Cloneable {
	private static final long serialVersionUID = 7227613056523385825L;
	private String products_sizes_id = null;
	private String ecommerce_size_id = null;
	private String dimensions = null;
	private int width_pixels = 0;
	private int height_pixels = 0;
	private int selected = 0;
	
	private int width = 0;
	private int height = 0;
	private String width_unit_id = null;
	private String height_unit_id = null;
	
	public String getProducts_sizes_id() {
		return products_sizes_id;
	}
	
	public void setProducts_sizes_id(String products_sizes_id) {
		this.products_sizes_id = products_sizes_id;
	}
	
	public String getEcommerce_size_id() {
		return ecommerce_size_id;
	}

	public void setEcommerce_size_id(String ecommerce_size_id) {
		this.ecommerce_size_id = ecommerce_size_id;
	}

	public int getSelected() {
		return selected;
	}

	public void setSelected(int selected) {
		this.selected = selected;
	}

	public String getDimensions() {
		return dimensions;
	}

	public void setDimensions(String dimensions) {
		this.dimensions = dimensions;
	}

	public void setWidth_pixels(int width_pixels) {
		this.width_pixels = width_pixels;
	}

	public int getWidth_pixels() {
		return width_pixels;
	}

	public void setHeight_pixels(int height_pixels) {
		this.height_pixels = height_pixels;
	}

	public int getHeight_pixels() {
		return height_pixels;
	}

	public String getWidth_unit_id() {
		return width_unit_id;
	}

	public void setWidth_unit_id(String width_unit_id) {
		this.width_unit_id = width_unit_id;
	}

	public String getHeight_unit_id() {
		return height_unit_id;
	}

	public void setHeight_unit_id(String height_unit_id) {
		this.height_unit_id = height_unit_id;
	}

	public double getSquareInches() {
		return width * height;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getWidth() {
		return width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getHeight() {
		return height;
	}	
	
	public SizeVO clone() throws CloneNotSupportedException {
		return (SizeVO) super.clone();
	}
}
