package com.biomed.smarttrak.fd;

import java.util.Date;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: FinancialDashRevenueDataRowVO.java<p/>
 * <b>Description: VO for importing, exporting, and updating scenario records</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Feb 20, 2018
 ****************************************************************************/

@Table(name="biomedgps_fd_scenario_overlay")
public class FinancialDashRevenueDataRowVO {

	private String overlayId;
	private String revenueId;
	private String scenarioId;
	private String scenarioName;
	private String companyName;
	private String companyId;
	private String sectionName;
	private String regionCode;
	private int yearNo;
	private long q1No;
	private long q2No;
	private long q3No;
	private long q4No;
	private Date createDate;
	private Date updateDate;

	@Column(name="overlay_id", isPrimaryKey=true)
	public String getOverlayId() {
		return overlayId;
	}
	public void setOverlayId(String overlayId) {
		this.overlayId = overlayId;
	}

	@Column(name="revenue_id")
	public String getRevenueId() {
		return revenueId;
	}
	public void setRevenueId(String revenueId) {
		this.revenueId = revenueId;
	}

	@Column(name="scenario_id")
	public String getScenarioId() {
		return scenarioId;
	}
	public void setScenarioId(String scenarioId) {
		this.scenarioId = scenarioId;
	}

	@Column(name="scenario_nm")
	public String getScenarioName() {
		return scenarioName;
	}
	public void setScenarioName(String scenarioName) {
		this.scenarioName = scenarioName;
	}
	@Column(name="company_nm", isReadOnly=true)
	public String getCompanyName() {
		return companyName;
	}
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	@Column(name="company_id")
	public String getCompanyId() {
		return companyId;
	}
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}
	
	@Column(name="section_nm", isReadOnly=true)
	public String getSectionName() {
		return sectionName;
	}
	public void setSectionName(String sectionName) {
		this.sectionName = sectionName;
	}

	@Column(name="region_cd", isReadOnly=true)
	public String getRegionCode() {
		return regionCode;
	}
	public void setRegionCode(String regionCode) {
		this.regionCode = regionCode;
	}

	@Column(name="year_no")
	public int getYearNo() {
		return yearNo;
	}
	public void setYearNo(int yearNo) {
		this.yearNo = yearNo;
	}

	@Column(name="q1_no")
	public long getQ1No() {
		return q1No;
	}
	public void setQ1No(long q1No) {
		this.q1No = q1No;
	}

	@Column(name="q2_no")
	public long getQ2No() {
		return q2No;
	}
	public void setQ2No(long q2No) {
		this.q2No = q2No;
	}

	@Column(name="q3_no")
	public long getQ3No() {
		return q3No;
	}
	public void setQ3No(long q3No) {
		this.q3No = q3No;
	}

	@Column(name="q4_no")
	public long getQ4No() {
		return q4No;
	}
	public void setQ4No(long q4No) {
		this.q4No = q4No;
	}

	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}
	
}
