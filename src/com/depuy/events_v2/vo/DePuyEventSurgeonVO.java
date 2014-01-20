package com.depuy.events_v2.vo;

import java.io.Serializable;
import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.gis.Location;

/****************************************************************************
 * <b>Title</b>: DePuyEventSurgeonVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 15, 2014
 ****************************************************************************/
public class DePuyEventSurgeonVO implements Serializable {

	private static final long serialVersionUID = -2688403965507062015L;
	private String surgeonId = null;
	private String eventPostcardId = null;
	private String surgeonName = null;
	private String cvFileUrl = null;
	private String logoFileUrl = null;
	private int seenGuidelinesFlg = 0;
	private int hospEmployeeFlg = 0;
	private String hospAddress = null;
	private int experienceYrs = 0;
	private String practiceName = null;
	private int practYrs = 0;
	private Location practLocation = null;  //add1, addr2, city, state, zip
	private String practPhone = null;
	private String practEmail = null;
	private String practWebsite = null;
	private String secPhone = null;
	private String secEmail = null;
	private String surgeonBio = null;
	
	public DePuyEventSurgeonVO() {
	}
	
	public DePuyEventSurgeonVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		setSurgeonName(db.getStringVal("surgeon_nm", rs));
		setPracticeName(db.getStringVal("practice_nm", rs));
		 surgeonId = db.getStringVal("depuy_event_surgeon_id", rs);
		 eventPostcardId = db.getStringVal("event_postcard_id", rs);
		surgeonName = db.getStringVal("surgeon_nm", rs);
		cvFileUrl = db.getStringVal("cv_file_url", rs);
		logoFileUrl = db.getStringVal("logo_file_url", rs);
		seenGuidelinesFlg = db.getIntVal("seen_guidelines_flg", rs);
		hospEmployeeFlg = db.getIntVal("hosp_employee_flg", rs);
		hospAddress = db.getStringVal("hosp_address_txt", rs);
		experienceYrs = db.getIntVal("exp_yrs_no", rs);
		practiceName = db.getStringVal("practice_nm", rs);
		practYrs = db.getIntVal("pract_yrs_no", rs);
		practPhone = db.getStringVal("pract_phone_txt", rs);
		practEmail = db.getStringVal("pract_email_txt", rs);
		practWebsite = db.getStringVal("pract_website_url", rs);
		secPhone = db.getStringVal("sec_phone_txt", rs);
		secEmail =  db.getStringVal("sec_email_txt", rs);
		surgeonBio =  db.getStringVal("bio_txt", rs);
		Location loc= new Location(db.getStringVal("pract_addr1_txt", rs), 
												db.getStringVal("pract_city_nm", rs), 
												db.getStringVal("pract_state_cd", rs),  
												db.getStringVal("pract_zip_cd", rs));
		loc.setAddress2(db.getStringVal("pract_addr2_txt", rs));
		setPractLocation(loc);
		
		db = null;
	}
	
	
	public String getSurgeonId() {
		return surgeonId;
	}
	public void setSurgeonId(String surgeonId) {
		this.surgeonId = surgeonId;
	}
	public String getEventPostcardId() {
		return eventPostcardId;
	}
	public void setEventPostcardId(String eventPostcardId) {
		this.eventPostcardId = eventPostcardId;
	}
	public String getSurgeonName() {
		return surgeonName;
	}
	public void setSurgeonName(String surgeonName) {
		this.surgeonName = surgeonName;
	}
	public String getCvFileUrl() {
		return cvFileUrl;
	}
	public void setCvFileUrl(String cvFileUrl) {
		this.cvFileUrl = cvFileUrl;
	}
	public String getLogoFileUrl() {
		return logoFileUrl;
	}
	public void setLogoFileUrl(String logoFileUrl) {
		this.logoFileUrl = logoFileUrl;
	}
	public int getSeenGuidelinesFlg() {
		return seenGuidelinesFlg;
	}
	public void setSeenGuidelinesFlg(int seenGuidelinesFlg) {
		this.seenGuidelinesFlg = seenGuidelinesFlg;
	}
	public int getHospEmployeeFlg() {
		return hospEmployeeFlg;
	}
	public void setHospEmployeeFlg(int hospEmployeeFlg) {
		this.hospEmployeeFlg = hospEmployeeFlg;
	}
	public String getHospAddress() {
		return hospAddress;
	}
	public void setHospAddress(String hospAddress) {
		this.hospAddress = hospAddress;
	}
	public int getExperienceYrs() {
		return experienceYrs;
	}
	public void setExperienceYrs(int experienceYrs) {
		this.experienceYrs = experienceYrs;
	}
	public String getPracticeName() {
		return practiceName;
	}
	public void setPracticeName(String practiceName) {
		this.practiceName = practiceName;
	}
	public int getPractYrs() {
		return practYrs;
	}
	public void setPractYrs(int practYrs) {
		this.practYrs = practYrs;
	}
	public Location getPractLocation() {
		return practLocation;
	}
	public void setPractLocation(Location practLocation) {
		this.practLocation = practLocation;
	}
	public String getPractPhone() {
		return practPhone;
	}
	public void setPractPhone(String practPhone) {
		this.practPhone = practPhone;
	}
	public String getPractEmail() {
		return practEmail;
	}
	public void setPractEmail(String practEmail) {
		this.practEmail = practEmail;
	}
	public String getPractWebsite() {
		return practWebsite;
	}
	public void setPractWebsite(String practWebsite) {
		this.practWebsite = practWebsite;
	}
	public String getSecPhone() {
		return secPhone;
	}
	public void setSecPhone(String secPhone) {
		this.secPhone = secPhone;
	}
	public String getSecEmail() {
		return secEmail;
	}
	public void setSecEmail(String secEmail) {
		this.secEmail = secEmail;
	}
	public String getSurgeonBio() {
		return surgeonBio;
	}
	public void setSurgeonBio(String surgeonBio) {
		this.surgeonBio = surgeonBio;
	}
	
}
