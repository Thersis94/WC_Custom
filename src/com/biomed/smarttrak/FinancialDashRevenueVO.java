package com.biomed.smarttrak;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: FinancialDashRevenueVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Feb 8, 2017
 ****************************************************************************/

@Table(name="BIOMEDGPS_FD_REVENUE")
public class FinancialDashRevenueVO extends SBModuleVO {
	
	private static final long serialVersionUID = 1L;
	private String revenueId;
	private String companyId;
	private String sectionId;
	private String regionCd;
	private int yearNo;
	private int q1No;
	private int q2No;
	private int q3No;
	private int q4No;
	private Date createDt;
	private Date updateDt;
	
	public FinancialDashRevenueVO() {
		super();
	}

	public FinancialDashRevenueVO(ResultSet rs) {
		this();
		setData(rs);
	}
	
	public FinancialDashRevenueVO(ActionRequest req) {
		this();
		setData(req);
	}
	
	/**
	 * Sets data from a ResultSet
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil util = new DBUtil();
		
		this.setRevenueId(util.getStringVal("REVENUE_ID", rs));
		this.setCompanyId(util.getStringVal("COMPANY_ID", rs));
		this.setSectionId(util.getStringVal("SECTION_ID", rs));
		this.setRegionCd(util.getStringVal("REGION_CD", rs));
		this.setYearNo(util.getIntVal("YEAR_NO", rs));
		this.setQ1No(util.getIntVal("Q1_NO", rs));
		this.setQ2No(util.getIntVal("Q2_NO", rs));
		this.setQ3No(util.getIntVal("Q3_NO", rs));
		this.setQ4No(util.getIntVal("Q4_NO", rs));
		this.setCreateDt(util.getDateVal("CREATE_DT", rs));
		this.setUpdateDt(util.getDateVal("UPDATE_DT", rs));
	}

	/**
	 * Sets data from the ActionRequest
	 * 
	 * @param req
	 */
	public void setData(ActionRequest req) {
		this.setRevenueId(req.getParameter("revenueId"));
		this.setCompanyId(req.getParameter("companyId"));
		this.setSectionId(req.getParameter("sectionId"));
		this.setRegionCd(req.getParameter("regionCd"));
		this.setYearNo(Convert.formatInteger(req.getParameter("yearNo")));
		this.setQ1No(Convert.formatInteger(req.getParameter("q1No")));
		this.setQ2No(Convert.formatInteger(req.getParameter("q2No")));
		this.setQ3No(Convert.formatInteger(req.getParameter("q3No")));
		this.setQ4No(Convert.formatInteger(req.getParameter("q4No")));
	}

	/**
	 * @return the revenueId
	 */
	@Column(name="REVENUE_ID", isPrimaryKey=true)
	public String getRevenueId() {
		return revenueId;
	}

	/**
	 * @return the companyId
	 */
	@Column(name="COMPANY_ID")
	public String getCompanyId() {
		return companyId;
	}

	/**
	 * @return the sectionId
	 */
	@Column(name="SECTION_ID")
	public String getSectionId() {
		return sectionId;
	}

	/**
	 * @return the regionCd
	 */
	@Column(name="REGION_CD")
	public String getRegionCd() {
		return regionCd;
	}

	/**
	 * @return the yearNo
	 */
	@Column(name="YEAR_NO")
	public int getYearNo() {
		return yearNo;
	}

	/**
	 * @return the q1No
	 */
	@Column(name="Q1_NO")
	public int getQ1No() {
		return q1No;
	}

	/**
	 * @return the q2No
	 */
	@Column(name="Q2_NO")
	public int getQ2No() {
		return q2No;
	}

	/**
	 * @return the q3No
	 */
	@Column(name="Q3_NO")
	public int getQ3No() {
		return q3No;
	}

	/**
	 * @return the q4No
	 */
	@Column(name="Q4_NO")
	public int getQ4No() {
		return q4No;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @return the updateDt
	 */
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDt() {
		return updateDt;
	}

	/**
	 * @param revenueId the revenueId to set
	 */
	public void setRevenueId(String revenueId) {
		this.revenueId = revenueId;
	}

	/**
	 * @param companyId the companyId to set
	 */
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	/**
	 * @param sectionId the sectionId to set
	 */
	public void setSectionId(String sectionId) {
		this.sectionId = sectionId;
	}

	/**
	 * @param regionCd the regionCd to set
	 */
	public void setRegionCd(String regionCd) {
		this.regionCd = regionCd;
	}

	/**
	 * @param yearNo the yearNo to set
	 */
	public void setYearNo(int yearNo) {
		this.yearNo = yearNo;
	}

	/**
	 * @param q1No the q1No to set
	 */
	public void setQ1No(int q1No) {
		this.q1No = q1No;
	}

	/**
	 * @param q2No the q2No to set
	 */
	public void setQ2No(int q2No) {
		this.q2No = q2No;
	}

	/**
	 * @param q3No the q3No to set
	 */
	public void setQ3No(int q3No) {
		this.q3No = q3No;
	}

	/**
	 * @param q4No the q4No to set
	 */
	public void setQ4No(int q4No) {
		this.q4No = q4No;
	}

	/**
	 * @param createDt the createDt to set
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	/**
	 * @param updateDt the updateDt to set
	 */
	public void setUpdateDt(Date updateDt) {
		this.updateDt = updateDt;
	}
}
