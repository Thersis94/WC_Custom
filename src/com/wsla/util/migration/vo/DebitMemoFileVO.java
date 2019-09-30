package com.wsla.util.migration.vo;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.annotations.Importable;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: DebitMemoVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> ***Change Me
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 23, 2019
 * @updates:
 ****************************************************************************/
public class DebitMemoFileVO {
	
	// Members
	private String ticketId;
	private String productOwner;
	private String serialNumber;
	private String retailerName;
	private String retailerId;
	private String retailerCreditMemoId;
	private String oemId;
	private Date initialContactDate;
	private Date userRefundDate;
	private Date purchaseDate;
	private Date oemAuthDate;
	private double refundCost;
	
	/**
	 * Retailer name to ID map
	 */
	private static Map<String, String> retailMap = new HashMap<>();
	static {
		retailMap.put("FAMSA", "3ec22037fdbcf78dac1002845ee33bb6");
		retailMap.put("HOME DEPOT", "20_HOMDEP");
		retailMap.put("CHEDRAHUI", "a38ededc4480a3afac100290da3effa8");
		retailMap.put("MAYORAMSA", "RETAILER_MAYORAMSA");
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}
	
	/**
	 * 
	 */
	public DebitMemoFileVO() {
		super();
	}

	/**
	 * @return the ticketId
	 */
	public String getTicketId() {
		return ticketId;
	}

	/**
	 * @return the productOwner
	 */
	public String getProductOwner() {
		return productOwner;
	}

	/**
	 * @return the serialNumber
	 */
	public String getSerialNumber() {
		return serialNumber;
	}

	/**
	 * @return the retailerName
	 */
	public String getRetailerName() {
		return retailerName;
	}

	/**
	 * @return the retailerCreditMemoId
	 */
	public String getRetailerCreditMemoId() {
		return retailerCreditMemoId;
	}

	/**
	 * @return the oemId
	 */
	public String getOemId() {
		return oemId;
	}

	/**
	 * @return the initialContactDate
	 */
	public Date getInitialContactDate() {
		return initialContactDate;
	}

	/**
	 * @return the userRefundDate
	 */
	public Date getUserRefundDate() {
		return userRefundDate;
	}

	/**
	 * @return the purchaseDate
	 */
	public Date getPurchaseDate() {
		return purchaseDate;
	}

	/**
	 * @return the oemAuthDate
	 */
	public Date getOemAuthDate() {
		return oemAuthDate;
	}

	/**
	 * @param ticketId the ticketId to set
	 */
	@Importable(name="S/O NUMBER")
	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	/**
	 * @param productOwner the productOwner to set
	 */
	@Importable(name="PRODUCT OWNER")
	public void setProductOwner(String productOwner) {
		this.productOwner = productOwner;
	}

	/**
	 * @param serialNumber the serialNumber to set
	 */
	@Importable(name="SERIAL NUMBER")
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	/**
	 * @param retailerName the retailerName to set
	 */
	@Importable(name="RETAILER")
	public void setRetailerName(String retailerName) {
		this.retailerName = retailerName;
		retailerId = retailMap.get(retailerName);
	}

	/**
	 * @param retailerCreditMemoId the retailerCreditMemoId to set
	 */
	@Importable(name="RETAILER CREDIT MEMO")
	public void setRetailerCreditMemoId(String retailerCreditMemoId) {
		this.retailerCreditMemoId = retailerCreditMemoId;
	}

	/**
	 * @param oemId the oemId to set
	 */
	@Importable(name="MFG")
	public void setOemId(String oemId) {
		this.oemId = oemId;
	}

	/**
	 * @param initialContactDate the initialContactDate to set
	 */
	@Importable(name="DATE OF INITIAL CONTACT")
	public void setInitialContactDate(Date initialContactDate) {
		this.initialContactDate = initialContactDate;
	}

	/**
	 * @param userRefundDate the userRefundDate to set
	 */
	@Importable(name="DATE RETAILERREFUNDED END USER")
	public void setUserRefundDate(Date userRefundDate) {
		this.userRefundDate = userRefundDate;
	}

	/**
	 * @param purchaseDate the purchaseDate to set
	 */
	@Importable(name="DATE PURCHASED")
	public void setPurchaseDate(Date purchaseDate) {
		this.purchaseDate = purchaseDate;
	}

	/**
	 * @param oemAuthDate the oemAuthDate to set
	 */
	@Importable(name="MFG AUTH DATE")
	public void setOemAuthDate(Date oemAuthDate) {
		this.oemAuthDate = oemAuthDate;
	}

	/**
	 * @return the retailerId
	 */
	public String getRetailerId() {
		return retailerId;
	}

	/**
	 * @param retailerId the retailerId to set
	 */
	public void setRetailerId(String retailerId) {
		this.retailerId = retailerId;
	}

	/**
	 * @return the refundCost
	 */
	public double getRefundCost() {
		return refundCost;
	}

	/**
	 * @param refundCost the refundCost to set
	 */
	@Importable(name="REFUND COST")
	public void setRefundCost(double refundCost) {
		this.refundCost = refundCost;
	}

}
