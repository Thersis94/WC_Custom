package com.ansmed.sb.physician;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/*****************************************************************************
 <p><b>Title</b>: ResourceVO.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since Feb 17, 2009
 Last Updated:
 ***************************************************************************/

public class ResourceVO extends AbstractSiteBuilderVO {
	private static final long serialVersionUID = 1l;
	private String resourceId = null;
	private String surgeonId = null;
	private Integer resourceTypeId =  null;
	private String resourceNm = null;
	private Integer usedQtr = new Integer(1);
	private Integer usedYear = new Integer(1);
	private Integer completionMonth = new Integer(1);
	private Integer completionYear = new Integer(1);
	private String resourceObj = null;
	private String resourceResult = null;
	private String repId = null;
	private String repFirstNm = null;
	private String repLastNm = null;
	private String surgeonFirstNm = null;
	private String surgeonLastNm = null;
	
	/**
	 * 
	 */
	public ResourceVO() {
		
	}
	
	/**
	 * Initializes the VO to the params provided in the row object
	 * @param rs
	 */
	public ResourceVO(ResultSet rs) {
		super();
		setData(rs);
	}
	
	/**
	 * Initializes the VO to the params provided in the request object
	 * @param rs
	 */
	public ResourceVO(ActionRequest req) {
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
		resourceId = db.getStringVal("resource_id", rs);
		surgeonId = db.getStringVal("surgeon_id", rs);
		resourceTypeId = db.getIntegerVal("resource_type_id", rs);
		resourceNm = db.getStringVal("resource_nm", rs);
		usedQtr = db.getIntegerVal("used_qtr_no",rs);
		usedYear = db.getIntegerVal("used_yr_no",rs);
		completionMonth = db.getIntegerVal("completion_month_no",rs);
		completionYear = db.getIntegerVal("completion_yr_no",rs);
		resourceObj = se.decodeValue(db.getStringVal("resource_obj_txt", rs));
		resourceResult = se.decodeValue(db.getStringVal("resource_result_txt", rs));
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
	public void setData(ActionRequest req) {
		resourceId = req.getParameter("resourceId");
		surgeonId = req.getParameter("surgeonId");
		resourceTypeId = Convert.formatInteger(req.getParameter("resourceTypeId"));
		resourceNm = req.getParameter("resourceNm");
		usedQtr = Convert.formatInteger(req.getParameter("usedQtr"));
		usedYear = Convert.formatInteger(req.getParameter("usedYear"));
		completionMonth = Convert.formatInteger(req.getParameter("completionMonth"));
		completionYear = Convert.formatInteger(req.getParameter("completionYear"));
		resourceObj = req.getParameter("resourceObj");
		resourceResult = req.getParameter("resourceResult");
	}

	/**
	 * @return the resourceId
	 */
	public String getResourceId() {
		return resourceId;
	}

	/**
	 * @param resourceId the resourceId to set
	 */
	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	/**
	 * @return the resourceObj
	 */
	public String getResourceObj() {
		return resourceObj;
	}

	/**
	 * @param resourceObj the resourceObj to set
	 */
	public void setResourceObj(String resourceObj) {
		this.resourceObj = resourceObj;
	}

	/**
	 * @return the resourceResult
	 */
	public String getResourceResult() {
		return resourceResult;
	}

	/**
	 * @param resourceResult the resourceResult to set
	 */
	public void setResourceResult(String resourceResult) {
		this.resourceResult = resourceResult;
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
	 * @return the resourceNm
	 */
	public String getResourceNm() {
		return resourceNm;
	}

	/**
	 * @param resourceNm the resourceNm to set
	 */
	public void setResourceNm(String resourceNm) {
		this.resourceNm = resourceNm;
	}

	/**
	 * @return the resourceTypeId
	 */
	public Integer getResourceTypeId() {
		return resourceTypeId;
	}

	/**
	 * @param resourceTypeId the resourceTypeId to set
	 */
	public void setResourceTypeId(Integer resourceTypeId) {
		this.resourceTypeId = resourceTypeId;
	}

	/**
	 * @return the usedQtr
	 */
	public Integer getUsedQtr() {
		return usedQtr;
	}

	/**
	 * @param usedQtr the usedQtr to set
	 */
	public void setUsedQtr(Integer usedQtr) {
		this.usedQtr = usedQtr;
	}

	/**
	 * @return the usedYear
	 */
	public Integer getUsedYear() {
		return usedYear;
	}

	/**
	 * @param usedYear the usedYear to set
	 */
	public void setUsedYear(Integer usedYear) {
		this.usedYear = usedYear;
	}

	/**
	 * @return the completionMonth
	 */
	public Integer getCompletionMonth() {
		return completionMonth;
	}

	/**
	 * @param completionMonth the completionMonth to set
	 */
	public void setCompletionMonth(Integer completionMonth) {
		this.completionMonth = completionMonth;
	}

	/**
	 * @return the completionYear
	 */
	public Integer getCompletionYear() {
		return completionYear;
	}

	/**
	 * @param completionYear the completionYear to set
	 */
	public void setCompletionYear(Integer completionYear) {
		this.completionYear = completionYear;
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
