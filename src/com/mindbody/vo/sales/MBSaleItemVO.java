package com.mindbody.vo.sales;

import java.math.BigDecimal;

/****************************************************************************
 * <b>Title:</b> MBPurchaseVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage MindBody SaleItem Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 25, 2017
 ****************************************************************************/
public class MBSaleItemVO {

	private boolean accountPayment;
	private BigDecimal amountPaid;
	private String description;
	private BigDecimal discount;
	private BigDecimal price;
	private int quantity;
	private boolean returned;
	private MBSaleVO sale;
	private BigDecimal tax;

	public MBSaleItemVO() {
		//Default Constructor
	}

	/**
	 * @return the accountPayment
	 */
	public boolean isAccountPayment() {
		return accountPayment;
	}

	/**
	 * @return the amountPaid
	 */
	public BigDecimal getAmountPaid() {
		return amountPaid;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the discount
	 */
	public BigDecimal getDiscount() {
		return discount;
	}

	/**
	 * @return the price
	 */
	public BigDecimal getPrice() {
		return price;
	}

	/**
	 * @return the quantity
	 */
	public int getQuantity() {
		return quantity;
	}

	/**
	 * @return the returned
	 */
	public boolean isReturned() {
		return returned;
	}

	/**
	 * @return the sale
	 */
	public MBSaleVO getSale() {
		return sale;
	}

	/**
	 * @return the tax
	 */
	public BigDecimal getTax() {
		return tax;
	}

	/**
	 * @param accountPayment the accountPayment to set.
	 */
	public void setAccountPayment(boolean accountPayment) {
		this.accountPayment = accountPayment;
	}

	/**
	 * @param amountPaid the amountPaid to set.
	 */
	public void setAmountPaid(BigDecimal amountPaid) {
		this.amountPaid = amountPaid;
	}

	/**
	 * @param description the description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param discount the discount to set.
	 */
	public void setDiscount(BigDecimal discount) {
		this.discount = discount;
	}

	/**
	 * @param price the price to set.
	 */
	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	/**
	 * @param quantity the quantity to set.
	 */
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	/**
	 * @param returned the returned to set.
	 */
	public void setReturned(boolean returned) {
		this.returned = returned;
	}

	/**
	 * @param sale the sale to set.
	 */
	public void setSale(MBSaleVO sale) {
		this.sale = sale;
	}

	/**
	 * @param tax the tax to set.
	 */
	public void setTax(BigDecimal tax) {
		this.tax = tax;
	}
}