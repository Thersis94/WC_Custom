package com.biomed.smarttrak;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: FinancialDashScenarioAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Jan 24, 2017
 ****************************************************************************/

public class FinancialDashScenarioAction extends SBActionAdapter {

	public FinancialDashScenarioAction() {
		super();
	}

	public FinancialDashScenarioAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	public void retrieve(ActionRequest req) throws ActionException {
		super.retrieve(req);

		List<FinancialDashScenarioVO> scenarios = new ArrayList<>();
		getScenarios(scenarios);
		
		this.putModuleData(scenarios);
	}
	
	/**
	 * Gets the list of scenarios available to the user
	 * 
	 * @param scenarios
	 */
	private void getScenarios(List<FinancialDashScenarioVO> scenarios) {
		String sql = getScenarioListSql();
		FinancialDashScenarioVO svo = null;
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			int idx = 0;
			
			// TODO: Where do I get the team/user values from?
			ps.setString(++idx, "3");
			ps.setString(++idx, "6614");
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				svo = new FinancialDashScenarioVO(rs);
				scenarios.add(svo);
			}
		} catch (SQLException sqle) {
			log.error("Unable to get financial dashboard scenario list ", sqle);
		}
	}
	
	/**
	 * Gets the sql for the user's scenario list
	 * 
	 * @return
	 */
	private String getScenarioListSql() {
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(500);
		
		sql.append("select * ");
		sql.append("from ").append(custom).append("BIOMEDGPS_FD_SCENARIO s ");
		sql.append("where team_id = ? or user_id = ? ");
		sql.append("order by scenario_nm ");
		
		return sql.toString();
	}
	
	public void build(ActionRequest req) throws ActionException {
		super.build(req);
		String scenarioName = StringUtil.checkVal(req.getParameter("scenarioName"));
		String scenarioRole = StringUtil.checkVal(req.getParameter("scenarioRole"));
		String updateType = StringUtil.checkVal(req.getParameter("type")); 

		log.debug("Editing Scenario: Name - " + scenarioName + " | Role - " + scenarioRole + " | Update Type: " + updateType);
	}
}
