package com.mindbody.vo.sales;

import java.math.BigDecimal;

/****************************************************************************
 * <b>Title:</b> MBPaymentVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage MindBody Payment data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 25, 2017
 ****************************************************************************/
public class MBPaymentVO {

	private BigDecimal amount;
	private long id;
	private String lastFour;
	private int method;
	private String notes;
	private String type;

	public MBPaymentVO() {
		//Default Constructor
	}

	/**
	 * @return the amount
	 */
	public BigDecimal getAmount() {
		return amount;
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return the lastFour
	 */
	public String getLastFour() {
		return lastFour;
	}

	/**
	 * @return the method
	 */
	public int getMethod() {
		return method;
	}

	/**
	 * @return the notes
	 */
	public String getNotes() {
		return notes;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param amount the amount to set.
	 */
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	/**
	 * @param id the id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @param lastFour the lastFour to set.
	 */
	public void setLastFour(String lastFour) {
		this.lastFour = lastFour;
	}

	/**
	 * @param method the method to set.
	 */
	public void setMethod(int method) {
		this.method = method;
	}

	/**
	 * @param notes the notes to set.
	 */
	public void setNotes(String notes) {
		this.notes = notes;
	}

	/**
	 * @param type the type to set.
	 */
	public void setType(String type) {
		this.type = type;
	}
}
