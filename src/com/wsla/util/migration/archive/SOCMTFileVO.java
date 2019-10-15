package com.wsla.util.migration.archive;

import java.util.Date;

import com.siliconmtn.annotations.Importable;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <p><b>Title:</b> SOCMTFileVO.java</p>
 * <p><b>Description:</b> Represents a row in the Comments (ZZ-OSCMT) file.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Aug 9, 2019
 * <b>Changes:</b>
 ****************************************************************************/
@Table(name="wsla_sw_cmt")
public class SOCMTFileVO {

	private String soNumber;
	private Date receivedDate;
	private String status;
	private String comment1;
	private String comment2;
	private String comment3;
	private String comment4;
	private String comment5;
	private String comment6;
	private String fileName;

	@Column(name="so_number", isPrimaryKey=true)
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
	@Column(name="comment_line_1")
	public String getComment1() {
		return comment1;
	}
	@Column(name="comment_line_2")
	public String getComment2() {
		return comment2;
	}
	@Column(name="comment_line_3")
	public String getComment3() {
		return comment3;
	}
	@Column(name="comment_line_4")
	public String getComment4() {
		return comment4;
	}
	@Column(name="comment_line_5")
	public String getComment5() {
		return comment5;
	}
	@Column(name="comment_line_6")
	public String getComment6() {
		return comment6;
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
	@Importable(name="Comment Line 1")
	public void setComment1(String comment1) {
		this.comment1 = comment1;
	}
	@Importable(name="Comment Line 2")
	public void setComment2(String comment2) {
		this.comment2 = comment2;
	}
	@Importable(name="Comment Line 3")
	public void setComment3(String comment3) {
		this.comment3 = comment3;
	}
	@Importable(name="Comment Line 4")
	public void setComment4(String comment4) {
		this.comment4 = comment4;
	}
	@Importable(name="Comment Line 5")
	public void setComment5(String comment5) {
		this.comment5 = comment5;
	}
	@Importable(name="Comment Line 6")
	public void setComment6(String comment6) {
		this.comment6 = comment6;
	}
	public void setFileName(String nm) {
		this.fileName = nm;
	}
}