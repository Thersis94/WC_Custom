package com.ansmed.sb.report;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

import com.siliconmtn.util.Convert;

/*****************************************************************************
 <p><b>Title</b>: FellowsSummaryVO.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since May 05, 2009
 Last Updated:
 ***************************************************************************/

public class FellowsSummaryVO extends AbstractSiteBuilderVO {
	private static final long serialVersionUID = 1l;
	private String repFirstNm = null;
	private String repLastNm = null;
	private String surgeonFirstNm = null;
	private String surgeonLastNm = null;
	private String programNm = null;
	private String fellowsNm = null;
	private String fellowsEmail = null;
	private String fellowsPhone = null;
	private Integer fellowsEndMonth = null;
	private Integer fellowsEndYear = null;
	private String fellowsPlan = null;
	private String specialtyNm = null;
	
	/**
	 * 
	 */
	public FellowsSummaryVO() {
	}
	
	/**
	 * Initializes the VO to the params provided in the row object
	 * @param rs
	 */
	public FellowsSummaryVO(ResultSet rs) {
		super();
		setData(rs);
	}
	
	/**
	 * Initializes the VO to the params provided in the request object
	 * @param rs
	 */
	public FellowsSummaryVO(SMTServletRequest req) {
		super();
		setData(req);
	}
	
	/**
	 * Sets the VO to the params provided in the row object
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		StringEncoder se = new StringEncoder();
		DBUtil db = new DBUtil();
		repFirstNm = se.decodeValue(db.getStringVal("rep_first_nm", rs));
		repLastNm = se.decodeValue(db.getStringVal("rep_last_nm", rs));
		surgeonFirstNm = se.decodeValue(db.getStringVal("phys_first_nm", rs));
		surgeonLastNm = se.decodeValue(db.getStringVal("phys_last_nm", rs));
		programNm = se.decodeValue(db.getStringVal("program_nm", rs));
		fellowsNm = se.decodeValue(db.getStringVal("fellows_nm", rs));
		fellowsEmail = db.getStringVal("fellows_email_addr_txt", rs);
		fellowsPhone = db.getStringVal("fellows_phone_no", rs);
		fellowsEndMonth = db.getIntegerVal("fellows_end_month_no", rs);
		fellowsEndYear = db.getIntegerVal("fellows_end_yr_no", rs);
		fellowsPlan = se.decodeValue(db.getStringVal("fellows_plan_txt", rs));
		specialtyNm = se.decodeValue(db.getStringVal("specialty_nm", rs));
	}
	
	/**
	 * Sets the VO to the params provided in the request object
	 * @param req
	 */
	public void setData(SMTServletRequest req) {
		StringEncoder se = new StringEncoder();
		repFirstNm = se.decodeValue(req.getParameter("repFirstNm"));
		repLastNm = se.decodeValue(req.getParameter("repLastNm"));
		surgeonFirstNm = se.decodeValue(req.getParameter("physFirstNm"));
		surgeonLastNm = se.decodeValue(req.getParameter("physLastNm"));
		programNm = se.decodeValue(req.getParameter("programNm"));
		fellowsNm = se.decodeValue(req.getParameter("fellowsNm"));
		fellowsEmail = req.getParameter("fellowsEmail");
		fellowsPhone = req.getParameter("fellowsPhone");
		fellowsEndMonth = Convert.formatInteger(req.getParameter("fellowsEndMonth"));
		fellowsEndYear = Convert.formatInteger(req.getParameter("fellowsEndYear"));
		fellowsPlan = se.decodeValue(req.getParameter("fellowsPlan"));
		specialtyNm = se.decodeValue(req.getParameter("specialtyNm"));
	}
	

	/**
	 * @return the repFirstNm
	 */
	public String getRepFirstNm() {
		return repFirstNm;
	}

	/**
	 * @param repFirstNm the repFirstNm to set
	 */
	public void setRepFirstNm(String repFirstNm) {
		this.repFirstNm = repFirstNm;
	}

	/**
	 * @return the repLastNm
	 */
	public String getRepLastNm() {
		return repLastNm;
	}

	/**
	 * @param repLastNm the repLastNm to set
	 */
	public void setRepLastNm(String repLastNm) {
		this.repLastNm = repLastNm;
	}

	/**
	 * @return the surgeonFirstNm
	 */
	public String getSurgeonFirstNm() {
		return surgeonFirstNm;
	}

	/**
	 * @param surgeonFirstNm the surgeonFirstNm to set
	 */
	public void setSurgeonFirstNm(String surgeonFirstNm) {
		this.surgeonFirstNm = surgeonFirstNm;
	}

	/**
	 * @return the surgeonLastNm
	 */
	public String getSurgeonLastNm() {
		return surgeonLastNm;
	}

	/**
	 * @param surgeonLastNm the surgeonLastNm to set
	 */
	public void setSurgeonLastNm(String surgeonLastNm) {
		this.surgeonLastNm = surgeonLastNm;
	}
	
	/**
	 * @return the programNm
	 */
	public String getProgramNm() {
		return programNm;
	}

	/**
	 * @param programNm the programNm to set
	 */
	public void setProgramNm(String programNm) {
		this.programNm = programNm;
	}

	/**
	 * @return the fellowsNm
	 */
	public String getFellowsNm() {
		return fellowsNm;
	}

	/**
	 * @param fellowsNm the fellowsNm to set
	 */
	public void setFellowsNm(String fellowsNm) {
		this.fellowsNm = fellowsNm;
	}

	/**
	 * @return the fellowsEmail
	 */
	public String getFellowsEmail() {
		return fellowsEmail;
	}

	/**
	 * @param fellowsEmail the fellowsEmail to set
	 */
	public void setFellowsEmail(String fellowsEmail) {
		this.fellowsEmail = fellowsEmail;
	}

	/**
	 * @return the fellowsPhone
	 */
	public String getFellowsPhone() {
		return fellowsPhone;
	}

	/**
	 * @param fellowsPhone the fellowsPhone to set
	 */
	public void setFellowsPhone(String fellowsPhone) {
		this.fellowsPhone = fellowsPhone;
	}

	/**
	 * @return the fellowsEndMonth
	 */
	public Integer getFellowsEndMonth() {
		return fellowsEndMonth;
	}

	/**
	 * @param fellowsEndMonth the fellowsEndMonth to set
	 */
	public void setFellowsEndMonth(Integer fellowsEndMonth) {
		this.fellowsEndMonth = fellowsEndMonth;
	}

	/**
	 * @return the fellowsPlan
	 */
	public String getFellowsPlan() {
		return fellowsPlan;
	}

	/**
	 * @param fellowsPlan the fellowsPlan to set
	 */
	public void setFellowsPlan(String fellowsPlan) {
		this.fellowsPlan = fellowsPlan;
	}

	/**
	 * @return the fellowsEndYear
	 */
	public Integer getFellowsEndYear() {
		return fellowsEndYear;
	}

	/**
	 * @param fellowsEndYear the fellowsEndYear to set
	 */
	public void setFellowsEndYear(Integer fellowsEndYear) {
		this.fellowsEndYear = fellowsEndYear;
	}

	/**
	 * @return the specialtyNm
	 */
	public String getSpecialtyNm() {
		return specialtyNm;
	}

	/**
	 * @param specialtyNm the specialtyNm to set
	 */
	public void setSpecialtyNm(String specialtyNm) {
		this.specialtyNm = specialtyNm;
	}
	
}
