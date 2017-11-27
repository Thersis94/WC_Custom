package com.mindbody.vo.sales;

import java.math.BigDecimal;

import com.mindbodyonline.clients.api._0_5_1.ActionCode;

/****************************************************************************
 * <b>Title:</b> MBServiceVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage MindBody Service Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 26, 2017
 ****************************************************************************/
public class MBServiceVO {
	private ActionCode.Enum action;
	private int count;
	private String id;
	private String name;
	private BigDecimal onlinePrice;
	private BigDecimal price;
	private double productId;
	private int programId;
	private BigDecimal taxIncluded;
	private BigDecimal taxRate;

	public MBServiceVO() {
		//Default Constructor
	}

	/**
	 * @return the action
	 */
	public ActionCode.Enum getAction() {
		return action;
	}

	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the onlinePrice
	 */
	public BigDecimal getOnlinePrice() {
		return onlinePrice;
	}

	/**
	 * @return the price
	 */
	public BigDecimal getPrice() {
		return price;
	}

	/**
	 * @return the productId
	 */
	public double getProductId() {
		return productId;
	}

	/**
	 * @return the programId
	 */
	public int getProgramId() {
		return programId;
	}

	/**
	 * @return the taxIncluded
	 */
	public BigDecimal getTaxIncluded() {
		return taxIncluded;
	}

	/**
	 * @return the taxRate
	 */
	public BigDecimal getTaxRate() {
		return taxRate;
	}

	/**
	 * @param action the action to set.
	 */
	public void setAction(ActionCode.Enum action) {
		this.action = action;
	}

	/**
	 * @param count the count to set.
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * @param id the id to set.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @param name the name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param onlinePrice the onlinePrice to set.
	 */
	public void setOnlinePrice(BigDecimal onlinePrice) {
		this.onlinePrice = onlinePrice;
	}

	/**
	 * @param price the price to set.
	 */
	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	/**
	 * @param productId the productId to set.
	 */
	public void setProductId(double productId) {
		this.productId = productId;
	}

	/**
	 * @param programId the programId to set.
	 */
	public void setProgramId(int programId) {
		this.programId = programId;
	}

	/**
	 * @param taxIncluded the taxIncluded to set.
	 */
	public void setTaxIncluded(BigDecimal taxIncluded) {
		this.taxIncluded = taxIncluded;
	}

	/**
	 * @param taxRate the taxRate to set.
	 */
	public void setTaxRate(BigDecimal taxRate) {
		this.taxRate = taxRate;
	}
}