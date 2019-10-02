package com.wsla.util.migration.vo;

import java.util.Date;

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
public class AssetPathVO {

	private String ticketId;
	private Date createDate;
	private String comment;
	private String filePath;
	private String attributeCode;
	private boolean alert; //flags priority comments

	public AssetPathVO() {
		super();
	}

	public String getTicketId() {
		return ticketId;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public String getComment() {
		return comment;
	}

	public String getFilePath() {
		return filePath;
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

	@Override
	public String toString() {
		return StringUtil.getToString(this, false, 0, ",");
	}
}
