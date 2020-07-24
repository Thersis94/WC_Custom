package com.biomed.smarttrak.fd;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.vo.TeamVO;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.session.SMTSession;
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

	public static final String PRIVATE = "";

	/**
	 * P = Private, T = Team, L = Locked;
	 */
	private enum StatusLevel {P, T, L}

	public FinancialDashScenarioAction() {
		super();
	}

	public FinancialDashScenarioAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		UserVO vo = (UserVO) req.getSession().getAttribute(Constants.USER_DATA);

		List<FinancialDashScenarioVO> scenarios = getScenarios(vo.getTeams(), vo.getUserId());
		putModuleData(scenarios);
	}

	/**
	 * Gets the scenarios available to the user.
	 * 
	 * @param scenarios
	 */
	private List<FinancialDashScenarioVO> getScenarios(List<TeamVO> teams, String userId) {
		List<FinancialDashScenarioVO> scenarios = new ArrayList<>();
		boolean getAll = teams == null && userId == null;

		// Teams could be null from the overloaded method below
		int teamsCount = 0;
		if (teams != null) {
			teamsCount = teams.size();
		}

		String sql = getScenarioSql(getAll, teamsCount);
		FinancialDashScenarioVO svo = null;

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			int idx = 0;

			if (!getAll) {
				ps.setString(++idx, userId);

				for (TeamVO team : teams) {
					ps.setString(++idx, team.getTeamId());
				}
			}

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				svo = new FinancialDashScenarioVO(rs);
				scenarios.add(svo);
			}
		} catch (SQLException sqle) {
			log.error("Unable to get financial dashboard scenario list ", sqle);
		}

		return scenarios;
	}

	/**
	 * Gets a single scenario
	 * 
	 * @param scenarios
	 */
	public FinancialDashScenarioVO getScenario(String scenarioId) {
		String sql = getSingleScenarioSql();
		FinancialDashScenarioVO svo = null;

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, scenarioId);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				svo = new FinancialDashScenarioVO(rs);
			}
		} catch (SQLException sqle) {
			log.error("Unable to get financial dashboard scenario list ", sqle);
		}

		return svo;
	}

	/**
	 * Gets all of the existing scenarios, for data updates & such.
	 * This should never be used to display a list to a user!!
	 * 
	 * @return
	 */
	protected List<FinancialDashScenarioVO> getScenarios() {
		return getScenarios(null, null);
	}

	/**
	 * Gets the sql to return a scenario list
	 * 
	 * @return
	 */
	private String getScenarioSql(boolean getAll, int teamsCount) {
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(200);

		sql.append(DBUtil.SELECT_FROM_STAR).append(custom).append("BIOMEDGPS_FD_SCENARIO s ");

		if (!getAll) {
			sql.append("where user_id = ? ");

			for (int i = 1; i <= teamsCount; i++) {
				if (i == 1) {
					sql.append("or team_id in (? ");
				} else {
					sql.append(",? ");
				}

				if (i == teamsCount) {
					sql.append(") ");
				}
			}
		}
		sql.append("order by lower(scenario_nm) asc ");
		log.debug(sql);
		
		return sql.toString();
	}

	/**
	 * Gets the sql to return a single scenario
	 * 
	 * @return
	 */
	private String getSingleScenarioSql() {
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(200);

		sql.append(DBUtil.SELECT_FROM_STAR).append(custom).append("BIOMEDGPS_FD_SCENARIO s ");
		sql.append("where scenario_id = ? ");
		log.debug(sql);
		
		return sql.toString();
	}

	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		super.build(req);

		FinancialDashScenarioVO svo = new FinancialDashScenarioVO(req);
		DBProcessor dbp = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));

		SMTSession ses = req.getSession();
		UserVO uvo = (UserVO) ses.getAttribute(Constants.USER_DATA);
		svo.setUserId(uvo.getUserId());

		if (PRIVATE.equals(svo.getTeamId())) {
			svo.setTeamId(null);
			svo.setStatusFlg(StatusLevel.P.toString());
		} else {
			svo.setStatusFlg(StatusLevel.T.toString());
		}
		

		try {
			if (req.hasParameter("isDelete")) {
				dbp.delete(svo);
			} else {
				dbp.save(svo);
			}
		} catch (Exception e) {
			throw new ActionException("Couldn't update/create scenario record.", e);
		}

		Map<String, Object> response = new HashMap<>();
		response.put("scenarioId", svo.getScenarioId());
		putModuleData(response, 0, false);
	}
}
