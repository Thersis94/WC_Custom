package com.wsla.util.migration.vo;

import java.util.Calendar;
import java.util.Date;

import com.siliconmtn.annotations.Importable;
import com.siliconmtn.util.StringUtil;
import com.wsla.util.migration.LegacyDataImporter;

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
	//added for phase 2
	private String comment1;
	private String comment2;
	private String comment3;
	private String comment4;
	private String comment5;
	private String comment6;
	private String comment7;
	private String comment8;
	private String comment9;
	private String comment10;
	private String comment11;
	private String comment12;
	private String comment13;
	private String comment14;
	private String comment15;
	private String comment16;
	private String comment17;
	private String comment18;
	private String comment19;


	public String getSoNumber() {
		return soNumber;
	}
	public Date getReceivedDate() {
		return LegacyDataImporter.toUTCDate(receivedDate);
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

	public String getComment1() {
		return comment1;
	}
	public String getComment2() {
		return comment2;
	}
	public String getComment3() {
		return comment3;
	}
	public String getComment4() {
		return comment4;
	}
	public String getComment5() {
		return comment5;
	}
	public String getComment6() {
		return comment6;
	}
	public String getComment7() {
		return comment7;
	}
	public String getComment8() {
		return comment8;
	}
	public String getComment9() {
		return comment9;
	}
	public String getComment10() {
		return comment10;
	}
	public String getComment11() {
		return comment11;
	}
	public String getComment12() {
		return comment12;
	}
	public String getComment13() {
		return comment13;
	}
	public String getComment14() {
		return comment14;
	}
	public String getComment15() {
		return comment15;
	}
	public String getComment16() {
		return comment16;
	}
	public String getComment17() {
		return comment17;
	}
	public String getComment18() {
		return comment18;
	}
	public String getComment19() {
		return comment19;
	}
	@Importable(name="Item Text 1")
	public void setComment1(String comment1) {
		this.comment1 = comment1;
	}
	@Importable(name="Item Text 2")
	public void setComment2(String comment2) {
		this.comment2 = comment2;
	}
	@Importable(name="Item Text 3")
	public void setComment3(String comment3) {
		this.comment3 = comment3;
	}
	@Importable(name="Item Text 4")
	public void setComment4(String comment4) {
		this.comment4 = comment4;
	}
	@Importable(name="Item Text 5")
	public void setComment5(String comment5) {
		this.comment5 = comment5;
	}
	@Importable(name="Item Text 6")
	public void setComment6(String comment6) {
		this.comment6 = comment6;
	}
	@Importable(name="Item Text 7")
	public void setComment7(String comment7) {
		this.comment7 = comment7;
	}
	@Importable(name="Item Text 8")
	public void setComment8(String comment8) {
		this.comment8 = comment8;
	}
	@Importable(name="Item Text 9")
	public void setComment9(String comment9) {
		this.comment9 = comment9;
	}
	@Importable(name="Item Text 10")
	public void setComment10(String comment10) {
		this.comment10 = comment10;
	}
	@Importable(name="Item Text 11")
	public void setComment11(String comment11) {
		this.comment11 = comment11;
	}
	@Importable(name="Item Text 12")
	public void setComment12(String comment12) {
		this.comment12 = comment12;
	}
	@Importable(name="Item Text 13")
	public void setComment13(String comment13) {
		this.comment13 = comment13;
	}
	@Importable(name="Item Text 14")
	public void setComment14(String comment14) {
		this.comment14 = comment14;
	}
	@Importable(name="Item Text 15")
	public void setComment15(String comment15) {
		this.comment15 = comment15;
	}
	@Importable(name="Item Text 16")
	public void setComment16(String comment16) {
		this.comment16 = comment16;
	}
	@Importable(name="Item Text 17")
	public void setComment17(String comment17) {
		this.comment17 = comment17;
	}
	@Importable(name="Item Text 18")
	public void setComment18(String comment18) {
		this.comment18 = comment18;
	}
	@Importable(name="Item Text 19")
	public void setComment19(String comment19) {
		this.comment19 = comment19;
	}


	/**
	 * Return the requested comment agnostically
	 * @param x
	 * @return
	 */
	private String getComment(int x) {
		switch (x) {
			case 19: return getComment19();
			case 18: return getComment18();
			case 17: return getComment17();
			case 16: return getComment16();
			case 15: return getComment15();
			case 14: return getComment14();
			case 13: return getComment13();
			case 12: return getComment12();
			case 11: return getComment11();
			case 10: return getComment10();
			case 9: return getComment9();
			case 8: return getComment8();
			case 7: return getComment7();
			case 6: return getComment6();
			case 5: return getComment5();
			case 4: return getComment4();
			case 3: return getComment3();
			case 2: return getComment2();
			case 1: return getComment1();
			default: return null;
		}
	}

	/**
	 * return a list of comments from this row of data that need to be written to the DB.
	 * @param seed - where to start iterating; after the 1st comment, which gets saved as description
	 * @return
	 */
	public String getSWComments() {
		StringBuilder comments = new StringBuilder(500);

		for (int x=1; x < 20; x++) {
			String cmt = getComment(x);
			if (!StringUtil.isEmpty(cmt) && !cmt.trim().isEmpty())
				comments.append(cmt).append(" ");
		}
		return comments.toString().trim();
	}
}