package com.wsla.util.migration.vo;

import java.util.Calendar;
import java.util.Date;

import com.siliconmtn.annotations.Importable;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <p><b>Title:</b> SOLineItemFileVO.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Feb 1, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class SOLNIFileVO {

	private String soNumber;
	private Date receivedDate;
	private int orderNo; //line number
	private String fromLocationId; //parts location (source of shipment if C=I or C=N; not relevant otherwise)
	private String code; //"C", S=Service (goes on ledger), I & N are parts/shipment.  Import as 1=1 (1part=1shipment)
	private int defectiveQnty;
	private String productId; //itemId
	private String desc1;
	private String desc2;
	private int qntyNeeded;
	private double unitPrice;
	private double amtBilled; //place in shipment.cost_no
	private int qntyCommitted;
	private int qntyBackordered;


	public String getSoNumber() {
		return soNumber;
	}
	public Date getReceivedDate() {
		return receivedDate;
	}
	public int getOrderNo() {
		return orderNo;
	}
	public String getFromLocationId() {
		return fromLocationId;
	}
	public String getCode() {
		return code;
	}
	public int getDefectiveQnty() {
		return defectiveQnty;
	}
	public String getProductId() {
		return StringUtil.checkVal(productId).trim();
	}
	public String getDesc1() {
		return desc1;
	}
	public String getDesc2() {
		return desc2;
	}
	public int getQntyNeeded() {
		return qntyNeeded;
	}
	public double getUnitPrice() {
		return unitPrice;
	}
	public double getAmtBilled() {
		return amtBilled;
	}
	public int getQntyCommitted() {
		return qntyCommitted;
	}
	public int getQntyBackordered() {
		return qntyBackordered;
	}

	public boolean isService() {
		return "S".equalsIgnoreCase(getCode());
	}
	public boolean isInventory() {
		return "I".equalsIgnoreCase(getCode());
	}
	public boolean isCreditMemo() {
		return "N".equalsIgnoreCase(getCode());
	}


	@Importable(name="SO Number")
	public void setSoNumber(String soNumber) {
		this.soNumber = soNumber;
	}
	@Importable(name="Date Received")
	public void setReceivedDate(Date receivedDate) {
		this.receivedDate = receivedDate;
	}
	@Importable(name="Line Number")
	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}
	@Importable(name="Parts Location")
	public void setFromLocationId(String fromLocationId) {
		this.fromLocationId = fromLocationId;
	}
	/**
	 * status "C"ode of line item
	 *  S=Service (not inventory)
	 *  I=Inventory
	 *  N=credit memo/refund (ingest into inventory)
	 * @param code
	 */
	@Importable(name="C")
	public void setCode(String code) {
		this.code = code;
	}
	@Importable(name="Defective Qty")
	public void setDefectiveQnty(int defectiveQnty) {
		this.defectiveQnty = defectiveQnty;
	}
	@Importable(name="Item ID")
	public void setProductId(String productId) {
		this.productId = productId;
	}
	@Importable(name="Desc 1")
	public void setDesc1(String desc1) {
		this.desc1 = desc1;
	}
	@Importable(name="Desc 2")
	public void setDesc2(String desc2) {
		this.desc2 = desc2;
	}
	@Importable(name="QTY/UNITS")
	public void setQntyNeeded(int qntyNeeded) {
		this.qntyNeeded = qntyNeeded;
	}
	@Importable(name="UNIT PRICE")
	public void setUnitPrice(double unitPrice) {
		this.unitPrice = unitPrice;
	}
	@Importable(name="Amount Billed")
	public void setAmtBilled(double amtBilled) {
		this.amtBilled = amtBilled;
	}
	@Importable(name="Qty Committed")
	public void setQntyCommitted(int qntyCommitted) {
		this.qntyCommitted = qntyCommitted;
	}
	@Importable(name="QTY BACKORDER")
	public void setQntyBackordered(int qntyBackordered) {
		this.qntyBackordered = qntyBackordered;
	}
	
	/**
	 * combine the line number as a factor of minutes to the recieved date to resemble some sort of chronological ordering of the rows
	 * @return
	 */
	public Date getChronoReceivedDate() {
		if (getOrderNo() == 0) return getReceivedDate();
		Calendar cal = Calendar.getInstance();
		cal.setTime(getReceivedDate());
		cal.add(Calendar.MINUTE, getOrderNo());
		return cal.getTime();
	}
}