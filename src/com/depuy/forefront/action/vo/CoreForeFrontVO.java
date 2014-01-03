/**
 * 
 */
package com.depuy.forefront.action.vo;

import java.io.Serializable;
import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;

/****************************************************************************
 * <b>Title</b>: ProgramImplVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 20, 2012
 ****************************************************************************/
public class CoreForeFrontVO  implements Serializable {
	private static final long serialVersionUID = 199987344598724L;

	private String hospitalId = null;
	private String hospitalNm = null;
	private String hospitalInstId = null;
	private String programId = null;
	private String programNm = null;
	private String contactNm = null;
	private String contactEmail = null;
	
	public CoreForeFrontVO() {
	}
	
	public CoreForeFrontVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		setHospitalId(db.getStringVal("hospital_id", rs));
		setHospitalInstId(db.getStringVal("hospital_inst_id", rs));
		setHospitalNm(db.getStringVal("hospital_nm", rs));
		setProgramId(db.getStringVal("program_id", rs));
		setProgramNm(db.getStringVal("program_nm", rs));
		setContactNm(db.getStringVal("contact_nm", rs));
		setContactEmail(db.getStringVal("contact_email_txt", rs));
	}
	

	public String getHospitalNm() {
		return hospitalNm;
	}

	public void setHospitalNm(String hospitalNm) {
		this.hospitalNm = hospitalNm;
	}

	public String getHospitalId() {
		return hospitalId;
	}

	public void setHospitalId(String hospitalId) {
		this.hospitalId = hospitalId;
	}

	public String getHospitalInstId() {
		return hospitalInstId;
	}

	public void setHospitalInstId(String hospitalInstId) {
		this.hospitalInstId = hospitalInstId;
	}

	public String getContactNm() {
		return contactNm;
	}

	public void setContactNm(String contactNm) {
		this.contactNm = contactNm;
	}

	public String getProgramNm() {
		return programNm;
	}

	public void setProgramNm(String programNm) {
		this.programNm = programNm;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	public String getProgramId() {
		return programId;
	}

	public void setProgramId(String programId) {
		this.programId = programId;
	}

}
