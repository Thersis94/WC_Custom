package com.depuy.forefront.action.vo;

import java.io.Serializable;
import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;

public class ProgramVO implements Serializable {
	
	private static final long serialVersionUID = 1123345L;
	private String programId = null;
	private String programName = null;
	private String fullProgramName = null;
	private String contactName = null;
	private String contactEmailText = null;
	
	
	public ProgramVO() {
		
	}
	
	public ProgramVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		programId = db.getStringVal("program_id", rs);
		programName = db.getStringVal("program_nm", rs);
		fullProgramName = db.getStringVal("PROGRAM_FULL_NM", rs);
		contactName = db.getStringVal("contact_nm", rs);
		contactEmailText = db.getStringVal("contact_email_txt", rs);
		db = null;
	}
	
	public ProgramVO(SMTServletRequest req) {
		if (req.hasParameter("programId")) programId = req.getParameter("programId");
		programName = req.getParameter("programName");
		fullProgramName = req.getParameter("fullProgramName");
		contactName = req.getParameter("contactName");
		contactEmailText = req.getParameter("contactEmailText");
	}
	
	/**
	 * @return the programId
	 */
	public String getProgramId() {
		return programId;
	}

	/**
	 * @param programId the programId to set
	 */
	public void setProgramId(String programId) {
		this.programId = programId;
	}

	/**
	 * @return the programName
	 */
	public String getProgramName() {
		return programName;
	}

	/**
	 * @param programName the programName to set
	 */
	public void setProgramName(String programName) {
		this.programName = programName;
	}

	/**
	 * @return the contactName
	 */
	public String getContactName() {
		return contactName;
	}

	/**
	 * @param contactName the contactName to set
	 */
	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	/**
	 * @return the contactEmailText
	 */
	public String getContactEmailText() {
		return contactEmailText;
	}

	/**
	 * @param contactEmailText the contactEmailText to set
	 */
	public void setContactEmailText(String contactEmailText) {
		this.contactEmailText = contactEmailText;
	}

	public String getFullProgramName() {
		return fullProgramName;
	}

	public void setFullProgramName(String fullProgramName) {
		this.fullProgramName = fullProgramName;
	}
}
