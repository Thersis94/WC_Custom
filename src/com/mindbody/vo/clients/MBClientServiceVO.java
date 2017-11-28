package com.mindbody.vo.clients;

import java.util.Date;

import com.mindbody.vo.classes.MBProgramVO;
import com.mindbodyonline.clients.api._0_5_1.ActionCode;

/****************************************************************************
 * <b>Title:</b> MBServiceVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage MindBody Service Data
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 25, 2017
 ****************************************************************************/
public class MBClientServiceVO {

	private ActionCode.Enum action;
	private Date activeDate;
	private int count;
	private boolean current;
	private Date expirationDate;
	private long id;
	private String name;
	private Date paymentDate;
	private MBProgramVO program;
	private int remaining;
	private int siteId;

	public MBClientServiceVO() {
		//Default constructor
	}

	/**
	 * @return the action
	 */
	public ActionCode.Enum getAction() {
		return action;
	}
	/**
	 * @return the activeDate
	 */
	public Date getActiveDate() {
		return activeDate;
	}
	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}
	/**
	 * @return the current
	 */
	public boolean isCurrent() {
		return current;
	}
	/**
	 * @return the expirationDate
	 */
	public Date getExpirationDate() {
		return expirationDate;
	}
	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return the paymentDate
	 */
	public Date getPaymentDate() {
		return paymentDate;
	}
	/**
	 * @return the program
	 */
	public MBProgramVO getProgram() {
		return program;
	}
	/**
	 * @return the remaining
	 */
	public int getRemaining() {
		return remaining;
	}
	/**
	 * @return the siteId
	 */
	public int getSiteId() {
		return siteId;
	}
	/**
	 * @param enum1 the action to set.
	 */
	public void setAction(ActionCode.Enum action) {
		this.action = action;
	}
	/**
	 * @param activeDate the activeDate to set.
	 */
	public void setActiveDate(Date activeDate) {
		this.activeDate = activeDate;
	}
	/**
	 * @param count the count to set.
	 */
	public void setCount(int count) {
		this.count = count;
	}
	/**
	 * @param current the current to set.
	 */
	public void setCurrent(boolean current) {
		this.current = current;
	}
	/**
	 * @param expirationDate the expirationDate to set.
	 */
	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}
	/**
	 * @param id the id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @param name the name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @param paymentDate the paymentDate to set.
	 */
	public void setPaymentDate(Date paymentDate) {
		this.paymentDate = paymentDate;
	}
	/**
	 * @param program the program to set.
	 */
	public void setProgram(MBProgramVO program) {
		this.program = program;
	}
	/**
	 * @param remaining the remaining to set.
	 */
	public void setRemaining(int remaining) {
		this.remaining = remaining;
	}
	/**
	 * @param siteId the siteId to set.
	 */
	public void setSiteId(int siteId) {
		this.siteId = siteId;
	}
}