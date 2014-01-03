package com.fastsigns.action.approval.vo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
/****************************************************************************
 * <b>Title</b>: AbstractChangeLogVO.java
 * <p/>
 * <b>Project</b>: SB_FastSigns
 * <p/>
 * <b>Description: </b> This class provides basic functionality to the Approval
 * ChangeLogs that all others will use.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Sept 20, 2012
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public abstract class AbstractChangeLogVO {
	public static final String FTS_CHANGELOG_ID = "FTS_CHANGELOG_ID";
	public static final String COMPONENT_ID = "COMPONENT_ID";
	public static final String TYPE_ID = "TYPE_ID";
	public static final String SUBMITTER_ID = "SUBMITTER_ID";
	public static final String DESC_TXT = "DESC_TXT";
	public static final String SUBMITTED_DT = "SUBMITTED_DT";
	public static final String STATUS_NO = "STATUS_NO";
	public static final String FRANCHISE_ID = "FRANCHISE_ID";
	public static final String REVIEWER_ID = "REVIEWER_ID";
	public static final String RESOLUTION_TXT = "RESOLUTION_TXT";
	public static final String MOD_NAME = "MOD_NAME";
	public static final String REVIEW_DT = "REVIEW_DT";
	public static final String UPDATE_DT = "UPDATE_DT";
	public static final String OPTION_DESC = "OPTION_DESC";
	public static final String DATE_FORMAT = "MM/dd/yyyy HH:mm";
	
	private String orgId = null;
	private String ftsChangelogId = null;
	private String componentId = null;
	private String typeId = null;
	private String submitterId = null;
	private String reviewerId = null;
	private Integer statusNo = 0;
	private String descTxt = null;
	private String resolutionTxt = null;
	private String franchiseId = null;
	private String modNm = null;
	private String modDescTxt = null;
	private String submitterNm = null;
	private Date reviewDt = null;
	private Date submittedDt = null;
	private Date updateDt = null;
	private Integer approvalType = null;

	public static enum Status {
		PENDING, APPROVED, DENIED
	}

	public AbstractChangeLogVO() {
	}

	/**
	 * Constructor that takes a request object and initializes based off that.
	 * Use caution as this only reads initial data used to create new rows in
	 * the changelog table and defaults status to pending.
	 * 
	 * @param req
	 *            contains the data for creating a new changelog
	 */
	public AbstractChangeLogVO(SMTServletRequest req) {
		HttpSession ses = req.getSession();
		UserRoleVO role = (UserRoleVO) ses.getAttribute(Constants.ROLE_DATA);
		componentId = req.getParameter("componentId");
		typeId = req.getParameter("cmpType");
		submitterId = role.getProfileId();
		descTxt = req.getParameter("subComments");
		resolutionTxt = req.getParameter("revComments");
		statusNo = Status.PENDING.ordinal();
		orgId = ((SiteVO) req.getAttribute("siteData")).getOrganizationId();
		approvalType = Convert.formatInteger(req.getParameter("approvalType"));
	}

	/**
	 * Constructor that takes a resultset to set data members
	 * 
	 * @param rs
	 *            contains the data for a particular changelog
	 * @throws SQLException
	 */
	public AbstractChangeLogVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		ftsChangelogId = db.getStringVal(FTS_CHANGELOG_ID, rs);
		typeId = db.getStringVal(TYPE_ID, rs);
		submitterId = db.getStringVal(SUBMITTER_ID, rs);
		descTxt = db.getStringVal(DESC_TXT, rs);
		submittedDt = db.getDateVal(SUBMITTED_DT, rs);
		statusNo = db.getIntegerVal(STATUS_NO, rs);
		updateDt = db.getDateVal(UPDATE_DT, rs);
		componentId = db.getStringVal(COMPONENT_ID, rs);
		modDescTxt = db.getStringVal(OPTION_DESC, rs);
	}

	/**
	 * Constructor that takes a map of values to set data members.
	 * 
	 * @param vals
	 */
	public AbstractChangeLogVO(Map<String, String> vals) {
		ftsChangelogId = vals.get(FTS_CHANGELOG_ID);
		componentId = vals.get(COMPONENT_ID);
		typeId = vals.get(TYPE_ID);
		submitterId = vals.get(SUBMITTER_ID);
		descTxt = vals.get(DESC_TXT);
		submittedDt = Convert.formatTimestamp(DATE_FORMAT,
				vals.get(SUBMITTED_DT));
		statusNo = Convert.formatInteger(vals.get(STATUS_NO));
		franchiseId = vals.get(FRANCHISE_ID);
		reviewerId = vals.get(REVIEWER_ID);
		resolutionTxt = vals.get(RESOLUTION_TXT);
		modNm = vals.get(MOD_NAME);
		reviewDt = Convert.formatTimestamp(DATE_FORMAT, vals.get(REVIEW_DT));
		updateDt = Convert.formatTimestamp(DATE_FORMAT, vals.get(UPDATE_DT));
	}

	/**
	 * Used by report to properly store table data.
	 * 
	 * @param rs
	 *            ResultSet that contains data for populating the table.
	 * @return The Changelog instance modified.
	 * @throws SQLException
	 */
	public abstract void setData(ResultSet rs);

	public String getFtsChangelogId() {
		return ftsChangelogId;
	}

	public void setFtsChangelogId(String ftsChangelogId) {
		this.ftsChangelogId = ftsChangelogId;
	}

	public String getComponentId() {
		return componentId;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}

	public String getTypeId() {
		return typeId;
	}

	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}

	public String getSubmitterId() {
		return submitterId;
	}

	public void setSubmitterId(String submitterId) {
		this.submitterId = submitterId;
	}

	public String getReviewerId() {
		return reviewerId;
	}

	public void setReviewerId(String reviewerId) {
		this.reviewerId = reviewerId;
	}

	public Integer getStatusNo() {
		return statusNo;
	}

	public void setStatusNo(Integer statusNo) {
		this.statusNo = statusNo;
	}

	public String getDescTxt() {
		return descTxt;
	}

	public void setDescTxt(String descTxt) {
		this.descTxt = descTxt;
	}

	public String getResolutionTxt() {
		return resolutionTxt;
	}

	public void setResolutionTxt(String resolutionTxt) {
		this.resolutionTxt = resolutionTxt;
	}

	public String getFranchiseId() {
		return franchiseId;
	}

	public void setFranchiseId(String franchiseId) {
		this.franchiseId = franchiseId;
	}

	public String getModName() {
		return modNm;
	}

	public void setModName(String modNm) {
		this.modNm = modNm;
	}

	public Date getReviewDate() {
		return reviewDt;
	}

	public void setReviewDate(Date reviewDt) {
		this.reviewDt = reviewDt;
	}

	public Date getSubmittedDate() {
		return submittedDt;
	}

	public void setSubmittedDate(Date submittedDt) {
		this.submittedDt = submittedDt;
	}

	public Date getUpdateDate() {
		return updateDt;
	}

	public void setUpdateDate(Date updateDt) {
		this.updateDt = updateDt;
	}

	public String getModDescTxt() {
		return modDescTxt;
	}

	public void setModDescTxt(String modDescTxt) {
		this.modDescTxt = modDescTxt;
	}

	public String getSubmitterName() {
		return submitterNm;
	}

	public void setSubmitterName(String submitterNm) {
		this.submitterNm = submitterNm;
	}

	public String getOrgId() {
		return orgId;
	}

	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}

	public void setApprovalType(Integer approvalType) {
		this.approvalType = approvalType;
	}

	public Integer getApprovalType() {
		return approvalType;
	}

	public String getFriendlyStatus() {
		return Status.values()[this.statusNo].toString().toLowerCase();
	}
	
	/**
	 * Returns classpath of the Action that Handles this type of changelog
	 * @return
	 */
	public abstract String getActionClassPath();

	/**
	 * Returns Human Friendly ChangeLog Type used in reporting.
	 * @return
	 */
	public abstract String getHFriendlyType();

}
