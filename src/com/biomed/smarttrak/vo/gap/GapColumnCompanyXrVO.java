/**
 *
 */
package com.biomed.smarttrak.vo.gap;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.http.SMTServletRequest;

/****************************************************************************
 * <b>Title</b>: GapColumnCompanyXrVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Gap Analysis Column Company XR VO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Jan 12, 2017
 ****************************************************************************/
@Table(name="BIOMEDGPS_GA_COLUMN_COMPANY_XR")
public class GapColumnCompanyXrVO {

	private String xrId;
	private String companyId;
	private String gaColumnId;
	private Date createDt;

	public GapColumnCompanyXrVO() {}
	public GapColumnCompanyXrVO(SMTServletRequest req) {
		setData(req);
	}
	public GapColumnCompanyXrVO(ResultSet rs) {
		setData(rs);
	}

	/**
	 * Set Gap Column Company XR Data off the Request
	 * @param req
	 */
	private void setData(SMTServletRequest req) {
		this.xrId = req.getParameter("xrId");
		this.companyId = req.getParameter("companyId");
		this.gaColumnId = req.getParameter("columnId");
	}

	/**
	 * Set Gap Column Company XR Data off the ResultSet
	 * @param rs
	 */
	private void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		this.xrId = db.getStringVal("XR_ID", rs);
		this.companyId = db.getStringVal("COMPANY_ID", rs);
		this.gaColumnId = db.getStringVal("GA_COLUMN_ID", rs);
		this.createDt = db.getDateVal("CREATE_DT", rs);
	}
	/**
	 * @return the xrId
	 */
	@Column(name="XR_ID", isPrimaryKey=true)
	public String getXrId() {
		return xrId;
	}
	/**
	 * @return the companyId
	 */
	@Column(name="COMPANY_ID")
	public String getCompanyId() {
		return companyId;
	}
	/**
	 * @return the gaColumnId
	 */
	@Column(name="GA_COLUMN_ID")
	public String getGaColumnId() {
		return gaColumnId;
	}
	/**
	 * @return the createDt
	 */
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}
	/**
	 * @param xrId the xrId to set.
	 */
	public void setXrId(String xrId) {
		this.xrId = xrId;
	}
	/**
	 * @param companyId the companyId to set.
	 */
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}
	/**
	 * @param gaColumnId the gaColumnId to set.
	 */
	public void setGaColumnId(String gaColumnId) {
		this.gaColumnId = gaColumnId;
	}
	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

}