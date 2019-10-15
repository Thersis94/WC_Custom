package com.wsla.util.migration.archive;

import java.util.Date;

import com.siliconmtn.annotations.Importable;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

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
@Table(name="wsla_sw_lni")
public class SOLNIFileVO {

	private String soNumber;
	private Date receivedDate;
	private int orderNo; //line number
	private String fromLocationId; //parts location (source of shipment if C=I or C=N; not relevant otherwise)
	private String code; //"C", S=Service (goes on ledger), I & N are parts/shipment.  Import as 1=1 (1part=1shipment)
	private String status;
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
	private String fileName;


	@Column(name="so_number")
	public String getSoNumber() {
		return soNumber;
	}
	@Column(name="date_received")
	public Date getReceivedDate() {
		return receivedDate;
	}
	@Column(name="status")
	public String getStatus() {
		return status;
	}
	@Column(name="line_number")
	public int getOrderNo() {
		return orderNo;
	}
	@Column(name="parts_location")
	public String getFromLocationId() {
		return fromLocationId;
	}
	@Column(name="c")
	public String getCode() {
		return code;
	}
	@Column(name="defective_qty")
	public int getDefectiveQnty() {
		return defectiveQnty;
	}
	@Column(name="item_id")
	public String getProductId() {
		return productId;
	}
	@Column(name="desc_1")
	public String getDesc1() {
		return desc1;
	}
	@Column(name="desc_2")
	public String getDesc2() {
		return desc2;
	}
	@Column(name="qty_units")
	public int getQntyNeeded() {
		return qntyNeeded;
	}
	@Column(name="unit_price")
	public double getUnitPrice() {
		return unitPrice;
	}
	@Column(name="amount_billed")
	public double getAmtBilled() {
		return amtBilled;
	}
	@Column(name="qty_committed")
	public int getQntyCommitted() {
		return qntyCommitted;
	}
	@Column(name="qty_backorder")
	public int getQntyBackordered() {
		return qntyBackordered;
	}
	@Column(name="item_text_1")
	public String getComment1() {
		return comment1;
	}
	@Column(name="item_text_2")
	public String getComment2() {
		return comment2;
	}
	@Column(name="item_text_3")
	public String getComment3() {
		return comment3;
	}
	@Column(name="item_text_4")
	public String getComment4() {
		return comment4;
	}
	@Column(name="item_text_5")
	public String getComment5() {
		return comment5;
	}
	@Column(name="item_text_6")
	public String getComment6() {
		return comment6;
	}
	@Column(name="item_text_7")
	public String getComment7() {
		return comment7;
	}
	@Column(name="item_text_8")
	public String getComment8() {
		return comment8;
	}
	@Column(name="item_text_9")
	public String getComment9() {
		return comment9;
	}
	@Column(name="item_text_10")
	public String getComment10() {
		return comment10;
	}
	@Column(name="item_text_11")
	public String getComment11() {
		return comment11;
	}
	@Column(name="item_text_12")
	public String getComment12() {
		return comment12;
	}
	@Column(name="item_text_13")
	public String getComment13() {
		return comment13;
	}
	@Column(name="item_text_14")
	public String getComment14() {
		return comment14;
	}
	@Column(name="item_text_15")
	public String getComment15() {
		return comment15;
	}
	@Column(name="item_text_16")
	public String getComment16() {
		return comment16;
	}
	@Column(name="item_text_17")
	public String getComment17() {
		return comment17;
	}
	@Column(name="item_text_18")
	public String getComment18() {
		return comment18;
	}
	@Column(name="item_text_19")
	public String getComment19() {
		return comment19;
	}
	@Column(name="file_name")
	public String getFileName() {
		return fileName;
	}

	@Importable(name="SO Number")
	public void setSoNumber(String soNumber) {
		this.soNumber = soNumber;
	}
	@Importable(name="Date Received")
	public void setReceivedDate(Date receivedDate) {
		this.receivedDate = receivedDate;
	}
	@Importable(name="Status")
	public void setStatus(String s) {
		this.status = s;
	}
	@Importable(name="Line Number")
	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}
	@Importable(name="Parts Location")
	public void setFromLocationId(String fromLocationId) {
		this.fromLocationId = fromLocationId;
	}
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
	public void setFileName(String nm) {
		this.fileName = nm;
	}
}