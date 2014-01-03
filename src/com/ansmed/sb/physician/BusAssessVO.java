package com.ansmed.sb.physician;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/*****************************************************************************
 <p><b>Title</b>: BusAssessVO.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since Feb 11, 2009
 Last Updated:
 ***************************************************************************/

public class BusAssessVO extends AbstractSiteBuilderVO {
	private static final long serialVersionUID = 1l;
	private String busAssessId = null;
	private Integer assessType = null;
	private String assessTxt = null;
	private String surgeonId = null;
	private String repId = null;
	private String repFirstNm = null;
	private String repLastNm = null;
	private String surgeonFirstNm = null;
	private String surgeonLastNm = null;
	
	/**
	 * 
	 */
	public BusAssessVO() {
		
	}
	
	/**
	 * Initializes the VO to the params provided in the row object
	 * @param rs
	 */
	public BusAssessVO(ResultSet rs) {
		super();
		setData(rs);
	}
	
	/**
	 * Initializes the VO to the params provided in the request object
	 * @param rs
	 */
	public BusAssessVO(SMTServletRequest req) {
		super();
		setData(req);
	}
	
	/**
	 * Sets the VO to the params provided in the row object
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		StringEncoder se = new StringEncoder();
		busAssessId = db.getStringVal("bus_assess_id", rs);
		assessType = db.getIntVal("bus_assess_type", rs);
		assessTxt = se.decodeValue(db.getStringVal("bus_assess_txt", rs));
		surgeonId = db.getStringVal("surgeon_id", rs);
		repId = db.getStringVal("sales_rep_id", rs);
		repFirstNm = se.decodeValue(db.getStringVal("rep_first_nm", rs));
		repLastNm = se.decodeValue(db.getStringVal("rep_last_nm", rs));
		surgeonFirstNm = se.decodeValue(db.getStringVal("phys_first_nm", rs));
		surgeonLastNm = se.decodeValue(db.getStringVal("phys_last_nm", rs));
	}
	
	/**
	 * Sets the VO to the params provided in the request object
	 * @param req
	 */
	public void setData(SMTServletRequest req) {
		busAssessId = req.getParameter("assessId");
		assessType = Convert.formatInteger(req.getParameter("assessType"));
		assessTxt = req.getParameter("assessTxt");
		surgeonId = req.getParameter("surgeonId");
	}

	/**
	 * @return the busAssessId
	 */
	public String getBusAssessId() {
		return busAssessId;
	}

	/**
	 * @param busAssessId the busAssessId to set
	 */
	public void setBusAssessId(String busAssessId) {
		this.busAssessId = busAssessId;
	}

	/**
	 * @return the surgeonId
	 */
	public String getSurgeonId() {
		return surgeonId;
	}

	/**
	 * @param surgeonId the surgeonId to set
	 */
	public void setSurgeonId(String surgeonId) {
		this.surgeonId = surgeonId;
	}

	/**
	 * @return the assessType
	 */
	public Integer getAssessType() {
		return assessType;
	}

	/**
	 * @param assessType the assessType to set
	 */
	public void setAssessType(Integer assessType) {
		this.assessType = assessType;
	}

	/**
	 * @return the assessTxt
	 */
	public String getAssessTxt() {
		return assessTxt;
	}

	/**
	 * @param assessTxt the assessTxt to set
	 */
	public void setAssessTxt(String assessTxt) {
		this.assessTxt = assessTxt;
	}

	/**
	 * @return the repId
	 */
	public String getRepId() {
		return repId;
	}

	/**
	 * @param repId the repId to set
	 */
	public void setRepId(String repId) {
		this.repId = repId;
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

}
