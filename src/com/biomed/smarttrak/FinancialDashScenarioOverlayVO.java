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
 * <b>Title</b>: FinancialDashScenarioOverlayVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Feb 8, 2017
 ****************************************************************************/

@Table(name="biomedgps_fd_scenario_overlay")
public class FinancialDashScenarioOverlayVO extends SBModuleVO {
	
	private static final long serialVersionUID = 1L;
	private String overlayId;
	private String companyId;
	private String scenarioId;
	private String revenueId;
	private int yearNo;
	private long q1No;
	private long q2No;
	private long q3No;
	private long q4No;
	private Date createDt;
	private Date updateDt;
	
	public FinancialDashScenarioOverlayVO() {
		super();
	}

	public FinancialDashScenarioOverlayVO(ResultSet rs) {
		this();
		setData(rs);
	}
	
	public FinancialDashScenarioOverlayVO(ActionRequest req) {
		this();
		setData(req);
	}
	
	public FinancialDashScenarioOverlayVO(FinancialDashRevenueVO rvo) {
		this();
		setData(rvo);
	}
	
	/**
	 * Sets data from a ResultSet
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil util = new DBUtil();
		
		this.setOverlayId(util.getStringVal("OVERLAY_ID", rs));
		this.setCompanyId(util.getStringVal("COMPANY_ID", rs));
		this.setScenarioId(util.getStringVal("SCENARIO_ID", rs));
		this.setRevenueId(util.getStringVal("REVENUE_ID", rs));
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
		this.setOverlayId(req.getParameter("overlayId"));
		this.setCompanyId(req.getParameter("companyId"));
		this.setScenarioId(req.getParameter("scenarioId"));
		this.setRevenueId(req.getParameter("revenueId"));
		this.setYearNo(Convert.formatInteger(req.getParameter("yearNo")));
		this.setQ1No(Convert.formatInteger(req.getParameter("q1No")));
		this.setQ2No(Convert.formatInteger(req.getParameter("q2No")));
		this.setQ3No(Convert.formatInteger(req.getParameter("q3No")));
		this.setQ4No(Convert.formatInteger(req.getParameter("q4No")));
	}
	
	/**
	 * Sets common data from a FinancialDashRevenueVO
	 * 
	 * @param rvo
	 */
	public void setData(FinancialDashRevenueVO rvo) {
		this.setCompanyId(rvo.getCompanyId());
		this.setRevenueId(rvo.getRevenueId());
		this.setYearNo(rvo.getYearNo());
		this.setQ1No(rvo.getQ1No());
		this.setQ2No(rvo.getQ2No());
		this.setQ3No(rvo.getQ3No());
		this.setQ4No(rvo.getQ4No());
	}

	/**
	 * @return the overlayId
	 */
	@Column(name="overlay_id", isPrimaryKey=true)
	public String getOverlayId() {
		return overlayId;
	}

	/**
	 * @return the companyId
	 */
	@Column(name="company_id")
	public String getCompanyId() {
		return companyId;
	}

	/**
	 * @return the scenarioId
	 */
	@Column(name="scenario_id")
	public String getScenarioId() {
		return scenarioId;
	}

	/**
	 * @return the revenueId
	 */
	@Column(name="revenue_id")
	public String getRevenueId() {
		return revenueId;
	}

	/**
	 * @return the yearNo
	 */
	@Column(name="year_no")
	public int getYearNo() {
		return yearNo;
	}

	/**
	 * @return the q1No
	 */
	@Column(name="q1_no")
	public long getQ1No() {
		return q1No;
	}

	/**
	 * @return the q2No
	 */
	@Column(name="q2_no")
	public long getQ2No() {
		return q2No;
	}

	/**
	 * @return the q3No
	 */
	@Column(name="q3_no")
	public long getQ3No() {
		return q3No;
	}

	/**
	 * @return the q4No
	 */
	@Column(name="q4_no")
	public long getQ4No() {
		return q4No;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @return the updateDt
	 */
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDt() {
		return updateDt;
	}

	/**
	 * @param overlayId the overlayId to set
	 */
	public void setOverlayId(String overlayId) {
		this.overlayId = overlayId;
	}

	/**
	 * @param companyId the companyId to set
	 */
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	/**
	 * @param scenarioId the scenarioId to set
	 */
	public void setScenarioId(String scenarioId) {
		this.scenarioId = scenarioId;
	}

	/**
	 * @param revenueId the revenueId to set
	 */
	public void setRevenueId(String revenueId) {
		this.revenueId = revenueId;
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
	public void setQ1No(long q1No) {
		this.q1No = q1No;
	}

	/**
	 * @param q2No the q2No to set
	 */
	public void setQ2No(long q2No) {
		this.q2No = q2No;
	}

	/**
	 * @param q3No the q3No to set
	 */
	public void setQ3No(long q3No) {
		this.q3No = q3No;
	}

	/**
	 * @param q4No the q4No to set
	 */
	public void setQ4No(long q4No) {
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
