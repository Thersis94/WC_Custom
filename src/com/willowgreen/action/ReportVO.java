package com.willowgreen.action;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: ReportVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 9, 2012
 ****************************************************************************/
public class ReportVO implements Serializable {
	private static final long serialVersionUID = -7128508840678601033L;
	private String profileId = null;
	private String dealerLocationId = null;
	private String funeralHomeName = null;
	private String gifterName = null;
	private Date enrolledDate = null;
	private Date firstEmailDate = null;
	private Date lastEmailDate = null;
	private Integer emailCnt = Integer.valueOf(0);
	private Integer allowCommFlg = Integer.valueOf(0);
	private UserDataVO user = null;
	private UserDataVO submitter = null;
	private String contactSubmittalId = null;
	private int recordNo = 0;
	private boolean isGatekeeper = false;
	
	//added for grief's healing journey
	private String deceasedName = null;
	private String deceasedDt = null;
	private String relationship = null;
	
	public ReportVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		profileId = db.getStringVal("profile_id", rs);
		dealerLocationId = db.getStringVal("dealer_location_id", rs);
		funeralHomeName = db.getStringVal("home_nm", rs);
		gifterName = db.getStringVal("gifter_nm", rs);
		firstEmailDate = db.getDateVal("first_dt", rs);
		lastEmailDate = db.getDateVal("last_dt", rs);
		enrolledDate = db.getDateVal("create_dt", rs);
		emailCnt = db.getIntegerVal("email_cnt", rs);
		allowCommFlg = db.getIntegerVal("allow_comm_flg", rs);
		setContactSubmittalId(db.getStringVal("contact_submittal_id", rs));
		recordNo = db.getIntVal("record_no", rs);
		isGatekeeper = Convert.formatBoolean(db.getStringVal("is_gatekeeper", rs));
		
		deceasedName = db.getStringVal("deceased_nm", rs);
		relationship = db.getStringVal("relationship", rs);
		deceasedDt = db.getStringVal("deceased_dt", rs);
		
		db = null;
	}
	
	public String getProfileId() {
		return profileId;
	}
	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}
	public String getDealerLocationId() {
		return dealerLocationId;
	}
	public void setDealerLocationId(String dealerLocationId) {
		this.dealerLocationId = dealerLocationId;
	}
	public String getFuneralHomeName() {
		return funeralHomeName;
	}
	public void setFuneralHomeName(String funeralHomeName) {
		this.funeralHomeName = funeralHomeName;
	}
	public String getGifterName() {
		return gifterName;
	}
	public void setGifterName(String gifterName) {
		this.gifterName = gifterName;
	}
	public Date getFirstEmailDate() {
		return firstEmailDate;
	}
	public void setFirstEmailDate(Date firstEmailDate) {
		this.firstEmailDate = firstEmailDate;
	}
	public Date getLastEmailDate() {
		return lastEmailDate;
	}
	public void setLastEmailDate(Date lastEmailDate) {
		this.lastEmailDate = lastEmailDate;
	}
	public Integer getEmailCnt() {
		return emailCnt;
	}
	public void setEmailCnt(Integer emailCnt) {
		this.emailCnt = emailCnt;
	}
	public Integer getAllowCommFlg() {
		return allowCommFlg;
	}
	public void setAllowCommFlg(Integer allowCommFlg) {
		this.allowCommFlg = allowCommFlg;
	}
	public UserDataVO getUser() {
		return user;
	}
	public void setUser(UserDataVO user) {
		this.user = user;
	}

	public UserDataVO getSubmitter() {
		return submitter;
	}

	public void setSubmitter(UserDataVO submitter) {
		this.submitter = submitter;
	}

	public String getContactSubmittalId() {
		return contactSubmittalId;
	}

	public void setContactSubmittalId(String contactSubmittalId) {
		this.contactSubmittalId = contactSubmittalId;
	}

	public String getDeceasedName() {
		return deceasedName;
	}

	public void setDeceasedName(String deceasedName) {
		this.deceasedName = deceasedName;
	}

	public String getDeceasedDt() {
		return deceasedDt;
	}

	public void setDeceasedDt(String deceasedDt) {
		this.deceasedDt = deceasedDt;
	}

	public String getRelationship() {
		return relationship;
	}

	public void setRelationship(String relationship) {
		this.relationship = relationship;
	}

	public int getRecordNo() {
		return recordNo;
	}

	public void setRecordNo(int recordNo) {
		this.recordNo = recordNo;
	}

	public Date getEnrolledDate() {
		return enrolledDate;
	}

	public void setEnrolledDate(Date enrolledDate) {
		this.enrolledDate = enrolledDate;
	}

	public boolean isGatekeeper() {
		return isGatekeeper;
	}

	public void setGatekeeper(boolean isGatekeeper) {
		this.isGatekeeper = isGatekeeper;
	}
	
	
}
