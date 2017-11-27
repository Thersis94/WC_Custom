package com.mindbody.vo.sales;

import java.util.Date;
import java.util.List;

import com.mindbody.vo.classes.MBLocationVO;

/****************************************************************************
 * <b>Title:</b> MBSaleVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage MindBody Sale Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 25, 2017
 ****************************************************************************/
public class MBSaleVO {
	private String clientId;
	private long id;
	private MBLocationVO location;
	private List<MBPaymentVO> payments;
	private List<MBPurchasedItemVO> purchasedItems;
	private Date saleDate;
	private Date saleDateTime;
	private Date saleTime;

	public MBSaleVO() {
		//Default constructor
	}

	/**
	 * @return the clientId
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return the location
	 */
	public MBLocationVO getLocation() {
		return location;
	}

	/**
	 * @return the payments
	 */
	public List<MBPaymentVO> getPayments() {
		return payments;
	}

	/**
	 * @return the purchasedItems
	 */
	public List<MBPurchasedItemVO> getPurchasedItems() {
		return purchasedItems;
	}

	/**
	 * @return the saleDate
	 */
	public Date getSaleDate() {
		return saleDate;
	}

	/**
	 * @return the saleDateTime
	 */
	public Date getSaleDateTime() {
		return saleDateTime;
	}

	/**
	 * @return the saleTime
	 */
	public Date getSaleTime() {
		return saleTime;
	}

	/**
	 * @param clientId the clientId to set.
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * @param id the id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @param location the location to set.
	 */
	public void setLocation(MBLocationVO location) {
		this.location = location;
	}

	/**
	 * @param payments the payments to set.
	 */
	public void setPayments(List<MBPaymentVO> payments) {
		this.payments = payments;
	}

	/**
	 * @param purchasedItems the purchasedItems to set.
	 */
	public void setPurchasedItems(List<MBPurchasedItemVO> purchasedItems) {
		this.purchasedItems = purchasedItems;
	}

	/**
	 * @param saleDate the saleDate to set.
	 */
	public void setSaleDate(Date saleDate) {
		this.saleDate = saleDate;
	}

	/**
	 * @param saleDateTime the saleDateTime to set.
	 */
	public void setSaleDateTime(Date saleDateTime) {
		this.saleDateTime = saleDateTime;
	}

	/**
	 * @param saleTime the saleTime to set.
	 */
	public void setSaleTime(Date saleTime) {
		this.saleTime = saleTime;
	}
}