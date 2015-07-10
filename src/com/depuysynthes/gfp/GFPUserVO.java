package com.depuysynthes.gfp;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;

public class GFPUserVO {
	
	private String userId;
	private String profileId;
	private int activeFlg;
	private String hospitalId;
	private String hospitalName;
	private String programId;
	private UserDataVO profile;
	
	public GFPUserVO(){
		
	}
	
	public GFPUserVO(SMTServletRequest req) {
		assignData(req);
	}
	
	public void assignData(SMTServletRequest req) {
		userId = req.getParameter("USER_ID");
		profileId = req.getParameter("PROFILE_ID");
		activeFlg = Convert.formatInteger(req.getParameter("ACTIVE_FLG"));
		hospitalId = req.getParameter("HOSPITAL_ID");
		hospitalName = req.getParameter("HOSPITAL_NM");
	}
	
	public GFPUserVO(ResultSet rs) {
		assignData(rs);
	}
	
	public void assignData(ResultSet rs) {
		DBUtil db = new DBUtil();
		userId = db.getStringVal("USER_ID", rs);
		profileId = db.getStringVal("PROFILE_ID", rs);
		activeFlg = db.getIntegerVal("ACTIVE_FLG", rs);
		hospitalId = db.getStringVal("HOSPITAL_ID", rs);
		hospitalName = db.getStringVal("HOSPITAL_NM", rs);
		db = null;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
		if (profile != null) profile.setProfileId(profileId);
	}

	public int getActiveFlg() {
		return activeFlg;
	}

	public void setActiveFlg(int activeFlg) {
		this.activeFlg = activeFlg;
	}

	public String getHospitalId() {
		return hospitalId;
	}

	public void setHospitalId(String hospitalId) {
		this.hospitalId = hospitalId;
	}

	public String getHospitalName() {
		return hospitalName;
	}

	public void setHospitalName(String hospitalName) {
		this.hospitalName = hospitalName;
	}

	public String getProgramId() {
		return programId;
	}

	public void setProgramId(String programId) {
		this.programId = programId;
	}

	public UserDataVO getProfile() {
		return profile;
	}

	public void setProfile(UserDataVO profile) {
		this.profile = profile;
	}

}
