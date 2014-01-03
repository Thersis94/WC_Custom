package com.ansmed.sb.physician;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

import com.siliconmtn.util.Convert;

/*****************************************************************************
 <p><b>Title</b>: FellowsSurgeonVO.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since Mar 8, 2009
 Last Updated:
 ***************************************************************************/

public class FellowsSurgeonVO extends AbstractSiteBuilderVO {
	private static final long serialVersionUID = 1l;
	private String fellowsSurgeonId = null;
	private String fellowsNm = null;
	private Integer specialtyId = null;
	private String fellowsEmail = null;
	private String fellowsPhone = null;
	private Integer fellowsStartMonth = null;
	private Integer fellowsStartYear = null;
	private Integer fellowsEndMonth = null;
	private Integer fellowsEndYear = null;
	private String fellowsPlan = null;
	private String fellowsEd = null;
	private String fellowsReps = null;
	
	/**
	 * 
	 */
	public FellowsSurgeonVO() {
	}
	
	/**
	 * Initializes the VO to the params provided in the row object
	 * @param rs
	 */
	public FellowsSurgeonVO(ResultSet rs) {
		super();
		setData(rs);
	}
	
	/**
	 * Initializes the VO to the params provided in the request object
	 * @param rs
	 */
	public FellowsSurgeonVO(SMTServletRequest req) {
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
		fellowsSurgeonId = db.getStringVal("fellows_surgeon_id", rs);
		fellowsNm = se.decodeValue(db.getStringVal("fellows_nm", rs));
		specialtyId = db.getIntVal("fellows_specialty_id", rs);
		fellowsEmail = db.getStringVal("fellows_email_addr_txt", rs);
		fellowsPhone = db.getStringVal("fellows_phone_no", rs);
		fellowsStartMonth = db.getIntegerVal("fellows_start_month_no", rs);
		fellowsStartYear = db.getIntegerVal("fellows_start_yr_no", rs);
		fellowsEndMonth = db.getIntegerVal("fellows_end_month_no", rs);
		fellowsEndYear = db.getIntegerVal("fellows_end_yr_no", rs);
		fellowsPlan = se.decodeValue(db.getStringVal("fellows_plan_txt", rs));
		fellowsEd = se.decodeValue(db.getStringVal("fellows_ed_txt", rs));
		setFellowsReps(db.getStringVal("fellows_rep_id", rs));
		
	}
	
	/**
	 * Sets the VO to the params provided in the request object
	 * @param req
	 */
	public void setData(SMTServletRequest req) {
		StringEncoder se = new StringEncoder();
		fellowsSurgeonId = req.getParameter("fellowsSurgeonId");
		fellowsNm = se.decodeValue(req.getParameter("fellowsNm"));
		specialtyId = Convert.formatInteger(req.getParameter("specialtyId"));
		fellowsEmail = req.getParameter("fellowsEmail");
		fellowsPhone = req.getParameter("fellowsPhone");
		fellowsStartMonth = Convert.formatInteger(req.getParameter("fellowsStartMonth"));
		fellowsStartYear = Convert.formatInteger(req.getParameter("fellowsStartYear"));
		fellowsEndMonth = Convert.formatInteger(req.getParameter("fellowsEndMonth"));
		fellowsEndYear = Convert.formatInteger(req.getParameter("fellowsEndYear"));
		fellowsPlan = se.decodeValue(req.getParameter("fellowsPlan"));
		fellowsEd = se.decodeValue(req.getParameter("fellowsEd"));
		setFellowsReps(req.getParameterValues("fellowsReps"));	
	}

	/**
	 * @return the fellowsSurgeonId
	 */
	public String getFellowsSurgeonId() {
		return fellowsSurgeonId;
	}

	/**
	 * @param fellowsSurgeonId the fellowsSurgeonId to set
	 */
	public void setFellowsSurgeonId(String fellowsSurgeonId) {
		this.fellowsSurgeonId = fellowsSurgeonId;
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
	 * @return the specialtyId
	 */
	public Integer getSpecialtyId() {
		return specialtyId;
	}

	/**
	 * @param specialtyId the specialtyId to set
	 */
	public void setSpecialtyId(Integer specialtyId) {
		this.specialtyId = specialtyId;
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
	 * @return the fellowsStartMonth
	 */
	public Integer getFellowsStartMonth() {
		return fellowsStartMonth;
	}

	/**
	 * @param fellowsStartMonth the fellowsStartMonth to set
	 */
	public void setFellowsStartMonth(Integer fellowsStartMonth) {
		this.fellowsStartMonth = fellowsStartMonth;
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
	 * @return the fellowsEd
	 */
	public String getFellowsEd() {
		return fellowsEd;
	}

	/**
	 * @param fellowsEd the fellowsEd to set
	 */
	public void setFellowsEd(String fellowsEd) {
		this.fellowsEd = fellowsEd;
	}

	/**
	 * @return the fellowsStartYear
	 */
	public Integer getFellowsStartYear() {
		return fellowsStartYear;
	}

	/**
	 * @param fellowsStartYear the fellowsStartYear to set
	 */
	public void setFellowsStartYear(Integer fellowsStartYear) {
		this.fellowsStartYear = fellowsStartYear;
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
	 * @return the fellowsReps
	 */
	public String getFellowsReps() {
		return fellowsReps;
	}

	/**
	 * @param fellowsReps the fellowsReps to set
	 */
	public void setFellowsReps(String fellowsReps) {
		this.fellowsReps = fellowsReps;
	}

	/**
	 * @param fellowsReps the fellowsReps to set
	 */
	public void setFellowsReps(String[] fellowsReps) {
		StringBuffer sb = new StringBuffer("");
		if (fellowsReps != null && fellowsReps.length > 0) {
			for (int i = 0; i < fellowsReps.length; i++) {
				if (fellowsReps[i].length() > 0) sb.append(fellowsReps[i]);
				if ((i + 1) < fellowsReps.length) sb.append(",");
			}
		}
		this.fellowsReps = sb.toString();
	}
}
