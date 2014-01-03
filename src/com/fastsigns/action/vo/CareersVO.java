package com.fastsigns.action.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

public class CareersVO implements Serializable {
	private static final long serialVersionUID = -5433447806749384216L;
	public static final Integer CORP_NUMBER = 0;
	public static enum hours {fullTime, partTime, temporary};
	private String jobPostingId = null;
	private String jobTitleNm = null;
	private String jobLocNm = null;
	private Integer jobHours = null;
	private String jobDesc = null;
	private String jobResp = null;
	private String jobExpReq = null;
	private String jobDsrdSkills = null;
	private String jobAdtlComments = null;
	private String jobContactEmail = null;
	private String organizationId = null;
	private String franchiseId = null;
	private String locationNm = null;
	private String corporateNm = null;
	private String jobAddressTxt = null;
	private String jobAddress2Txt = null;
	private String jobCityNm = null;
	private String jobStateCd = null;
	private String jobCountryCd = null;
	private String jobZipCd = null;
	private String jobPrimaryPhoneNo = null;
	private Integer franchiseLinkFlg = null;
	private Date jobPostDt = null;
	private Integer jobApprovalFlg = 0;

	public CareersVO(){
		
	}
	
	public CareersVO(SMTServletRequest req){
		jobPostingId = req.getParameter("jobPostingId");
		franchiseId = req.getParameter("franchiseId");
		if(franchiseId != null && franchiseId.length() == 0)
			franchiseId = null;
		organizationId = req.getParameter("organizationId");
		jobTitleNm = req.getParameter("jobTitleNm");
		jobLocNm = req.getParameter("jobLocNm");
		jobHours = Convert.formatInteger(req.getParameter("jobHours"));
		jobDesc = StringUtil.checkVal(req.getParameter("jobDesc"));
		jobResp = StringUtil.checkVal(req.getParameter("jobResp"));
		jobExpReq = StringUtil.checkVal(req.getParameter("jobExpReq"));
		jobDsrdSkills = StringUtil.checkVal(req.getParameter("jobDsrdSkills"));
		jobAdtlComments = StringUtil.checkVal(req.getParameter("jobAdtlComments"));
		jobContactEmail = req.getParameter("jobContactEmail");
		jobPostDt = Convert.formatDate(req.getParameter("jobPostDt"));
		jobApprovalFlg = Convert.formatInteger(req.getParameter("jobApprovalFlg"));
		jobAddressTxt = req.getParameter("jobAddressTxt");
		jobAddress2Txt = req.getParameter("jobAddress2Txt");
		jobCityNm = req.getParameter("jobCityNm");
		jobStateCd = req.getParameter("jobStateCd");
		jobCountryCd = req.getParameter("jobCountryCd");
		jobZipCd = req.getParameter("jobZipCd");
		jobPrimaryPhoneNo = req.getParameter("jobPrimaryPhoneNo");
		franchiseLinkFlg = Convert.formatInteger(req.getParameter("franchiseLinkFlg"));
	}
	
	public CareersVO(ResultSet rs){
		DBUtil db = new DBUtil();
		jobPostingId = db.getStringVal("JOB_POSTING_ID", rs);
		franchiseId = db.getStringVal("FRANCHISE_ID", rs);
		organizationId = db.getStringVal("ORGANIZATION_ID", rs);
		jobTitleNm = db.getStringVal("JOB_TITLE_NM", rs);
		jobLocNm = db.getStringVal("JOB_LOC_NM", rs);
		jobHours = db.getIntegerVal("JOB_HRS", rs);
		jobDesc = db.getStringVal("JOB_DESC", rs);
		jobResp = db.getStringVal("JOB_RESP", rs);
		jobExpReq = db.getStringVal("JOB_EXP_REQ", rs);
		jobDsrdSkills = db.getStringVal("JOB_DSRD_SKILLS", rs);
		jobAdtlComments = db.getStringVal("JOB_ADTL_COMMENTS", rs);
		jobContactEmail = db.getStringVal("JOB_CONTACT_EMAIL", rs);
		jobPostDt = db.getDateVal("JOB_POST_DT", rs);
		jobApprovalFlg = db.getIntegerVal("JOB_APPROVAL_FLG", rs);
		locationNm = db.getStringVal("LOCATION_NM", rs);
		corporateNm = db.getStringVal("ATTRIB2_TXT", rs);
		jobAddressTxt = db.getStringVal("JOB_ADDRESS_TXT", rs);
		jobAddress2Txt = db.getStringVal("JOB_ADDRESS2_TXT", rs);
		jobCityNm = db.getStringVal("JOB_CITY_NM", rs);
		jobStateCd = db.getStringVal("JOB_STATE_CD", rs);
		jobCountryCd = db.getStringVal("JOB_COUNTRY_CD", rs);
		jobZipCd = db.getStringVal("JOB_ZIP_CD", rs);
		jobPrimaryPhoneNo = db.getStringVal("JOB_PRIMARY_PHONE_NO", rs);
		franchiseLinkFlg = db.getIntegerVal("FRANCHISE_LINK_FLG", rs);
	}

	/**
	 * @return the jobPostingId
	 */
	public String getJobPostingId() {
		return jobPostingId;
	}

	/**
	 * @param jobPostingId the jobPostingId to set
	 */
	public void setJobPostingId(String jobPostingId) {
		this.jobPostingId = jobPostingId;
	}

	/**
	 * @return the jobTitleNm
	 */
	public String getJobTitleNm() {
		return jobTitleNm;
	}

	/**
	 * @param jobTitleNm the jobTitleNm to set
	 */
	public void setJobTitleNm(String jobTitleNm) {
		this.jobTitleNm = jobTitleNm;
	}

	/**
	 * @return the jobLocNm
	 */
	public String getJobLocNm() {
		return jobLocNm;
	}

	/**
	 * @param jobLocNm the jobLocNm to set
	 */
	public void setJobLocNm(String jobLocNm) {
		this.jobLocNm = jobLocNm;
	}

	/**
	 * @return the jobHours
	 */
	public Integer getJobHours() {
		return jobHours;
	}

	/**
	 * @param jobHours the jobHours to set
	 */
	public void setJobHours(Integer jobHours) {
		this.jobHours = jobHours;
	}

	/**
	 * @return the jobDesc
	 */
	public String getJobDesc() {
		return jobDesc;
	}

	/**
	 * @param jobDesc the jobDesc to set
	 */
	public void setJobDesc(String jobDesc) {
		this.jobDesc = jobDesc;
	}

	/**
	 * @return the jobResp
	 */
	public String getJobResp() {
		return jobResp;
	}

	/**
	 * @param jobResp the jobResp to set
	 */
	public void setJobResp(String jobResp) {
		this.jobResp = jobResp;
	}

	/**
	 * @return the jobExpReq
	 */
	public String getJobExpReq() {
		return jobExpReq;
	}

	/**
	 * @param jobExpReq the jobExpReq to set
	 */
	public void setJobExpReq(String jobExpReq) {
		this.jobExpReq = jobExpReq;
	}

	/**
	 * @return the jobDsrdSkills
	 */
	public String getJobDsrdSkills() {
		return jobDsrdSkills;
	}

	/**
	 * @param jobDsrdSkills the jobDsrdSkills to set
	 */
	public void setJobDsrdSkills(String jobDsrdSkills) {
		this.jobDsrdSkills = jobDsrdSkills;
	}

	/**
	 * @return the jobAdtlComments
	 */
	public String getJobAdtlComments() {
		return jobAdtlComments;
	}

	/**
	 * @param jobAdtlComments the jobAdtlComments to set
	 */
	public void setJobAdtlComments(String jobAdtlComments) {
		this.jobAdtlComments = jobAdtlComments;
	}

	/**
	 * @return the jobContactEmail
	 */
	public String getJobContactEmail() {
		return jobContactEmail;
	}

	/**
	 * @param jobContactEmail the jobContactEmail to set
	 */
	public void setJobContactEmail(String jobContactEmail) {
		this.jobContactEmail = jobContactEmail;
	}

	/**
	 * @return the organizationId
	 */
	public String getOrganizationId() {
		return organizationId;
	}

	/**
	 * @param organizationId the organizationId to set
	 */
	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	/**
	 * @return the franchiseId
	 */
	public String getFranchiseId() {
		return franchiseId;
	}

	/**
	 * @param franchiseId the franchiseId to set
	 */
	public void setFranchiseId(String franchiseId) {
		this.franchiseId = franchiseId;
	}

	/**
	 * @return the jobPostDt
	 */
	public Date getJobPostDt() {
		return jobPostDt;
	}

	/**
	 * @param jobPostDt the jobPostDt to set
	 */
	public void setJobPostDt(Date jobPostDt) {
		this.jobPostDt = jobPostDt;
	}

	/**
	 * @return the jobApprovalFlg
	 */
	public Integer getJobApprovalFlg() {
		return jobApprovalFlg;
	}

	/**
	 * @param jobApprovalFlg the jobApprovalFlg to set
	 */
	public void setJobApprovalFlg(Integer jobApprovalFlg) {
		this.jobApprovalFlg = jobApprovalFlg;
	}

	/**
	 * @return the locationNm
	 */
	public String getLocationNm() {
		return locationNm;
	}

	/**
	 * @param locationNm the locationNm to set
	 */
	public void setLocationNm(String locationNm) {
		this.locationNm = locationNm;
	}

	/**
	 * @param corporateNm the corporateNm to set
	 */
	public void setCorporateNm(String corporateNm) {
		this.corporateNm = corporateNm;
	}

	/**
	 * @return the corporateNm
	 */
	public String getCorporateNm() {
		return corporateNm;
	}

	/**
	 * @return the jobAddressTxt
	 */
	public String getJobAddressTxt() {
		return jobAddressTxt;
	}

	/**
	 * @param jobAddressTxt the jobAddressTxt to set
	 */
	public void setJobAddressTxt(String jobAddressTxt) {
		this.jobAddressTxt = jobAddressTxt;
	}

	/**
	 * @return the jobAddress2Txt
	 */
	public String getJobAddress2Txt() {
		return jobAddress2Txt;
	}

	/**
	 * @param jobAddress2Txt the jobAddress2Txt to set
	 */
	public void setJobAddress2Txt(String jobAddress2Txt) {
		this.jobAddress2Txt = jobAddress2Txt;
	}

	/**
	 * @return the jobCityNm
	 */
	public String getJobCityNm() {
		return jobCityNm;
	}

	/**
	 * @param jobCityNm the jobCityNm to set
	 */
	public void setJobCityNm(String jobCityNm) {
		this.jobCityNm = jobCityNm;
	}

	/**
	 * @return the jobStateCd
	 */
	public String getJobStateCd() {
		return jobStateCd;
	}

	/**
	 * @param jobStateCd the jobStateCd to set
	 */
	public void setJobStateCd(String jobStateCd) {
		this.jobStateCd = jobStateCd;
	}

	/**
	 * @return the jobZipCd
	 */
	public String getJobZipCd() {
		return jobZipCd;
	}

	/**
	 * @param jobZipCd the jobZipCd to set
	 */
	public void setJobZipCd(String jobZipCd) {
		this.jobZipCd = jobZipCd;
	}

	/**
	 * @return the jobPrimaryPhoneNo
	 */
	public String getJobPrimaryPhoneNo() {
		return jobPrimaryPhoneNo;
	}

	/**
	 * @param jobPrimaryPhoneNo the jobPrimaryPhoneNo to set
	 */
	public void setJobPrimaryPhoneNo(String jobPrimaryPhoneNo) {
		this.jobPrimaryPhoneNo = jobPrimaryPhoneNo;
	}

	/**
	 * @return the franchiseLinkFlg
	 */
	public Integer getFranchiseLinkFlg() {
		return franchiseLinkFlg;
	}

	/**
	 * @param franchiseLinkFlg the franchiseLinkFlg to set
	 */
	public void setFranchiseLinkFlg(Integer franchiseLinkFlg) {
		this.franchiseLinkFlg = franchiseLinkFlg;
	}

	public void setJobCountryCd(String jobCountryCd) {
		this.jobCountryCd = jobCountryCd;
	}

	public String getJobCountryCd() {
		return jobCountryCd;
	}
		
}
