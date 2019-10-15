package com.wsla.util.migration.vo;

import java.util.Date;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <p><b>Title:</b> AssetPathVO.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Sep 12, 2019
 * <b>Changes:</b>
 ****************************************************************************/
@Table(name="wsla_sw_asset")
public class AssetPathVO {

	private String ticketId;
	private Date createDate;
	private String comment;
	private String filePath;
	private String attributeCode;
	private boolean alert; //flags priority comments
	private String fileName;

	public AssetPathVO() {
		super();
	}

	@Column(name="so_number")
	public String getTicketId() {
		return ticketId;
	}

	@Column(name="date_created")
	public Date getCreateDate() {
		return createDate;
	}

	@Column(name="comment")
	public String getComment() {
		return comment;
	}

	@Column(name="file_path")
	public String getFilePath() {
		return filePath;
	}

	@Column(name="is_alert")
	public int getAlert() {
		return isAlert() ? 1 : 0;
	}

	@Column(name="file_name")
	public String getFileName() {
		return fileName;
	}

	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public void setCreateDate(String stDate) {
		if (StringUtil.isEmpty(stDate)) return;
		this.createDate = Convert.formatDate("ddMMMyy", stDate);
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	@Column(name="attribute_code")
	public String getAttributeCode() {
		return attributeCode;
	}

	public void setAttributeCode(String attributeCode) {
		this.attributeCode = attributeCode;
	}

	public void isAlert(boolean alert) {
		this.alert = alert;
	}

	public boolean isAlert() {
		return this.alert;
	}

	public void setFileName(String nm) {
		this.fileName = nm;
	}

	@Override
	public String toString() {
		return StringUtil.getToString(this, false, 0, ",");
	}
}
