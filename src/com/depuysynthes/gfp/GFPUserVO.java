package com.depuysynthes.gfp;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;

public class GFPUserVO {
	
	private String userId;
	private int activeFlg;
	private String hospitalId;
	private String hospitalName;
	private String programId;
	private UserDataVO profile;
	
	public GFPUserVO(){
		
	}
	
	public GFPUserVO(ActionRequest req) {
		assignData(req);
	}
	
	public void assignData(ActionRequest req) {
		userId = req.getParameter("userId");
		activeFlg = Convert.formatInteger(req.getParameter("activeFlg"));
		hospitalId = req.getParameter("hospitalId");
		hospitalName = req.getParameter("hospitalName");
		programId = req.getParameter("programId");
		profile = new UserDataVO(req);
	}
	
	public GFPUserVO(ResultSet rs) {
		assignData(rs);
	}
	
	public void assignData(ResultSet rs) {
		DBUtil db = new DBUtil();
		userId = db.getStringVal("USER_ID", rs);
		activeFlg = db.getIntegerVal("ACTIVE_FLG", rs);
		hospitalId = db.getStringVal("HOSPITAL_ID", rs);
		hospitalName = db.getStringVal("HOSPITAL_NM", rs);
		programId = db.getStringVal("PROGRAM_ID", rs);
		profile = new UserDataVO(rs);
		db = null;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public int getActiveFlg() {
		return activeFlg;
	}

	public void setActiveFlg(int activeFlg) {
		this.activeFlg = activeFlg;
	}

	public boolean isActive() {
		return Convert.formatBoolean(activeFlg);
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
