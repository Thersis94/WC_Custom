/**
 *
 */
package com.biomed.smarttrak.vo;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.db.DBUtil;

/****************************************************************************
 * <b>Title</b>: GapCompanyVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO Manages all the data for a Gap Company Record.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since Feb 6, 2017
 ****************************************************************************/
public class GapCompanyVO {

	private Map<String, Map<String, RegulationVO>> regulations;
	private String companyName;
	private String companyId;
	private int portfolioNo;

	public GapCompanyVO() {
		regulations = new HashMap<>();
	}

	public GapCompanyVO(ResultSet rs) {
		this();
		setData(rs);
	}

	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		companyName = db.getStringVal("company_nm", rs);
		companyId = db.getStringVal("company_id", rs);
	}

	/**
	 * @return the regulations
	 */
	public Map<String, Map<String, RegulationVO>> getRegulations() {
		return regulations;
	}

	/**
	 * @return the companyName
	 */
	public String getCompanyName() {
		return companyName;
	}

	/**
	 * @return the companyId
	 */
	public String getCompanyId() {
		return companyId;
	}

	/**
	 * @param regulations the regulations to set.
	 */
	public void setRegulations(Map<String, Map<String, RegulationVO>> regulations) {
		this.regulations = regulations;
	}

	/**
	 * @param companyName the companyName to set.
	 */
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	/**
	 * @param companyId the companyId to set.
	 */
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	/**
	 * Retrieve the Portfolio No.
	 * @return
	 */
	public int getPortfolioNo() {
		return this.portfolioNo;
	}

	public void addRegulation(String columnId, RegulationVO r) {
		Map<String, RegulationVO> reg = regulations.get(columnId);
		if(reg == null) {
			reg = new HashMap<>();
		}

		//Add the Regulation to the proper Columns Regs.
		reg.put(r.getRegulatorId(), r);

		//Put Regulations back on map.
		regulations.put(columnId, reg);

		//Increment Portfolio No
		this.portfolioNo++;
	}

	public Map<String, RegulationVO> getRegulation(String columnId) {
		return regulations.get(columnId);
	}

	public boolean hasRegulation(String columnId) {
		return getRegulation(columnId) != null;
	}
}
