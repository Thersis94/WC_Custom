package com.biomed.smarttrak.admin.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.biomed.smarttrak.admin.AuditLogAction.AuditStatus;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: AuditLogVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Mar 10, 2017
 ****************************************************************************/

@Table(name="BIOMEDGPS_AUDIT_LOG")
public class AuditLogVO extends SBModuleVO {
	
	private static final long serialVersionUID = 1L;
	private String auditLogId;
	private String companyId;
	private String auditorProfileId;
	private AuditStatus statusCd;
	private Date startDt;
	private Date completeDt;
	private Date updateDt;
	private String companyNm;
	private String companyStatusNo;
	private Date companyUpdateDt;
	
	public AuditLogVO() {
		super();
	}

	public AuditLogVO(ResultSet rs) {
		this();
		setData(rs);
	}
	
	public AuditLogVO(ActionRequest req) {
		this();
		setData(req);
	}
	
	/**
	 * Sets data from a ResultSet
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil util = new DBUtil();
		
		setAuditLogId(util.getStringVal("AUDIT_LOG_ID", rs));
		setCompanyId(util.getStringVal("COMPANY_ID", rs));
		setAuditorProfileId(util.getStringVal("AUDITOR_PROFILE_ID", rs));
		setStatusCdTxt(util.getStringVal("STATUS_CD", rs));
		setStartDt(util.getDateVal("START_DT", rs));
		setCompleteDt(util.getDateVal("COMPLETE_DT", rs));
		setUpdateDt(util.getDateVal("UPDATE_DT", rs));
		setCompanyNm(util.getStringVal("COMPANY_NM", rs));
		setCompanyStatusNo(util.getStringVal("STATUS_NO", rs));
		setCompanyUpdateDt(util.getDateVal("COMPANY_UPDATE_DT", rs));
	}

	/**
	 * Sets data from the ActionRequest
	 * 
	 * @param req
	 */
	public void setData(ActionRequest req) {
		setAuditLogId(req.getParameter("auditLogId"));
		setCompanyId(req.getParameter("companyId"));
		setAuditorProfileId(req.getParameter("auditorProfileId"));
		setStatusCdTxt(req.getParameter("statusCd"));
	}

	/**
	 * @return the auditLogId
	 */
	@Column(name="audit_log_id", isPrimaryKey=true)
	public String getAuditLogId() {
		return auditLogId;
	}

	/**
	 * @return the companyId
	 */
	@Column(name="company_id")
	public String getCompanyId() {
		return companyId;
	}

	/**
	 * @return the auditorProfileId
	 */
	@Column(name="auditor_profile_id")
	public String getAuditorProfileId() {
		return auditorProfileId;
	}

	/**
	 * @return the statusCd
	 */
	public AuditStatus getStatusCd() {
		return statusCd;
	}

	/**
	 * @return the statusCd
	 */
	@Column(name="status_cd")
	public String getStatusCdTxt() {
		return (statusCd != null) ? statusCd.toString() : null;
	}

	/**
	 * @return the startDt
	 */
	@Column(name="start_dt", isAutoGen=true, isInsertOnly=true)
	public Date getStartDt() {
		return startDt;
	}

	/**
	 * @return the completeDt
	 */
	@Column(name="complete_dt")
	public Date getCompleteDt() {
		return completeDt;
	}

	/**
	 * @return the updateDt
	 */
	@Column(name="update_dt", isAutoGen=true)
	public Date getUpdateDt() {
		return updateDt;
	}

	/**
	 * @return the companyNm
	 */
	@Column(name="company_nm", isReadOnly=true)
	public String getCompanyNm() {
		return companyNm;
	}

	/**
	 * @return the companyStatusNo
	 */
	@Column(name="status_no", isReadOnly=true)
	public String getCompanyStatusNo() {
		return companyStatusNo;
	}

	/**
	 * @return the companyUpdateDt
	 */
	@Column(name="company_update_dt", isReadOnly=true)
	public Date getCompanyUpdateDt() {
		return companyUpdateDt;
	}

	/**
	 * @param auditLogId the auditLogId to set
	 */
	public void setAuditLogId(String auditLogId) {
		this.auditLogId = auditLogId;
	}

	/**
	 * @param companyId the companyId to set
	 */
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	/**
	 * @param auditorProfileId the auditorProfileId to set
	 */
	public void setAuditorProfileId(String auditorProfileId) {
		this.auditorProfileId = auditorProfileId;
	}

	/**
	 * @param statusCd the statusCd to set
	 */
	public void setStatusCd(AuditStatus statusCd) {
		this.statusCd = statusCd;
	}

	/**
	 * @param statusCd the statusCd to set
	 */
	public void setStatusCdTxt(String statusCd) {
		if (!StringUtil.isEmpty(statusCd))
			setStatusCd(AuditStatus.valueOf(statusCd));
	}

	/**
	 * @param startDt the startDt to set
	 */
	public void setStartDt(Date startDt) {
		this.startDt = startDt;
	}

	/**
	 * @param completeDt the completeDt to set
	 */
	public void setCompleteDt(Date completeDt) {
		this.completeDt = completeDt;
	}

	/**
	 * @param updateDt the updateDt to set
	 */
	public void setUpdateDt(Date updateDt) {
		this.updateDt = updateDt;
	}

	/**
	 * @param companyNm the companyNm to set
	 */
	public void setCompanyNm(String companyNm) {
		this.companyNm = companyNm;
	}

	/**
	 * @param companyStatusNo the companyStatusNo to set
	 */
	public void setCompanyStatusNo(String companyStatusNo) {
		this.companyStatusNo = companyStatusNo;
	}

	/**
	 * @param companyUpdateDt the companyUpdateDt to set
	 */
	public void setCompanyUpdateDt(Date companyUpdateDt) {
		this.companyUpdateDt = companyUpdateDt;
	}
}
