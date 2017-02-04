package com.biomed.smarttrak;

import java.sql.ResultSet;
import java.util.Random;

import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: FinancialDashScenarioVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Jan 25, 2017
 ****************************************************************************/

public class FinancialDashScenarioVO extends SBModuleVO {
	
	private static final long serialVersionUID = 1L;
	private String scenarioId;
	private String scenarioName;
	private String scenarioRole;
	
	public FinancialDashScenarioVO() {
		super();
	}

	public FinancialDashScenarioVO(ResultSet rs) {
		super(rs);
		setData(rs);
	}
	
	/**
	 * Sets data from a ResultSet
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		// TODO - Complete this method
	}
	
	// TODO: Remove this after there is real data to work with.
	public void setTempData() {
		Random rand = new Random();
		UUIDGenerator uuidGen = new UUIDGenerator();

		this.setScenarioId(uuidGen.getUUID());
		this.setScenarioName("Scenario " + rand.nextInt(25));
		this.setScenarioRole("test");
	}

	/**
	 * @return the scenarioId
	 */
	public String getScenarioId() {
		return scenarioId;
	}

	/**
	 * @return the scenarioName
	 */
	public String getScenarioName() {
		return scenarioName;
	}

	/**
	 * @return the scenarioRole
	 */
	public String getScenarioRole() {
		return scenarioRole;
	}

	/**
	 * @param scenarioId the scenarioId to set
	 */
	public void setScenarioId(String scenarioId) {
		this.scenarioId = scenarioId;
	}

	/**
	 * @param scenarioName the scenarioName to set
	 */
	public void setScenarioName(String scenarioName) {
		this.scenarioName = scenarioName;
	}

	/**
	 * @param scenarioRole the scenarioRole to set
	 */
	public void setScenarioRole(String scenarioRole) {
		this.scenarioRole = scenarioRole;
	}

}
