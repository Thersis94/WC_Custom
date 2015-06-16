package com.depuy.events_v2.vo;

import java.io.Serializable;
import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;

/****************************************************************************
 * <b>Title</b>: ConsigneeVO.java<p/>
 * <b>Description: For Co-Funded only, represents someone accountable for paying for the seminar.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 15, 2014
 ****************************************************************************/
public class ConsigneeVO implements Serializable {
	
	private static final long serialVersionUID = 388709879390927423L;

	private String consigneeId = null;
	private int typeNo = 0;
	private String partyName = null;
	private String contactName = null;
	private String title = null;
	private String phone = null;
	private String email = null;
	
	public ConsigneeVO() {
	}
	
	public ConsigneeVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		consigneeId = db.getStringVal("consignee_id", rs);
		typeNo = db.getIntVal("type_no", rs);
		partyName = db.getStringVal("party_nm", rs);
		contactName = db.getStringVal("contact_nm", rs);
		title = db.getStringVal("title_txt", rs);
		phone = db.getStringVal("phone_txt", rs);
		email = db.getStringVal("email_txt", rs);
		db = null;
	}

	public String getConsigneeId() {
		return consigneeId;
	}

	public void setConsigneeId(String consigneeId) {
		this.consigneeId = consigneeId;
	}

	public int getTypeNo() {
		return typeNo;
	}

	public void setTypeNo(int typeNo) {
		this.typeNo = typeNo;
	}

	public String getPartyName() {
		return partyName;
	}

	public void setPartyName(String partyName) {
		this.partyName = partyName;
	}

	public String getContactName() {
		return contactName;
	}

	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}