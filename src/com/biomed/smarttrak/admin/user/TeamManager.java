package com.biomed.smarttrak.admin.user;

// Java 7
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Log4j
import org.apache.log4j.Logger;

// WC_Custom
import com.biomed.smarttrak.vo.TeamVO;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.util.StringUtil;

// WebCrescendo
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: TeamManager.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author groot
 @version 1.0
 @since Feb 2, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class TeamManager extends AbstractManager {

	private Logger log = Logger.getLogger(TeamManager.class);
	private String userId;
	private String teamId;
	private String accountId;
	
	/**
	* Constructor
	*/
	public TeamManager() {
		// constructor stub
	}
	
	public TeamManager(Connection dbConn) {
		setDbConn(dbConn);
		setAttributes(new HashMap<String,Object>());
		
	}
	
	public TeamManager(Connection dbConn, Map<String,Object> attributes) {
		this(dbConn);
		setAttributes(attributes);
		
	}
	
	/**
	 * Retrieves a list of teams depending upon the fields that have been set (e.g. account ID, user ID, etc.)
	 * @param searchParams
	 * @return
	 * @throws ActionException
	 */
	public List<TeamVO> retrieveTeams() 
			throws ActionException {
		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(325);
		sql.append("select a.account_id, a.team_id, a.team_nm, a.default_flg, a.private_flg, "); 
		sql.append("a.create_dt, a.update_dt, b.user_id, b.create_dt as assigned_dt from ");
		sql.append(schema).append("biomedgps_team a inner join ");
		sql.append(schema).append("biomedgps_user_team_xr b ");
		sql.append("on a.team_id = b.team_id ");
		if (accountId != null) sql.append("and a.account_id = ? ");
		if (teamId != null) sql.append("and a.team_id = ? ");
		sql.append("where 1=1 ");
		if (userId != null) sql.append("and b.user_id = ? ");
		sql.append("order by a.team_nm ");
		log.debug("SmartTRAK teams SQL: " + sql.toString());

		StringBuilder errMsg = new StringBuilder(100);
		errMsg.append("Error retrieving SmartTRAK teams: ");
		
		int idx = 1;
		List<TeamVO> teams = new ArrayList<>();
		try (PreparedStatement ps = getDbConn().prepareStatement(sql.toString())) {

			if (accountId != null) ps.setString(idx++, accountId);
			if (teamId != null) ps.setString(idx++, teamId);
			if (userId != null) ps.setString(idx, userId);

			ResultSet rs = ps.executeQuery();

			TeamVO team;
			while (rs.next()) {
				team = new TeamVO(rs);
				teams.add(team);
			}
		} catch (SQLException sqle) {
			errMsg.append(sqle.getMessage());
			throw new ActionException(errMsg.toString());
		}
		
		return teams;
	}

	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = StringUtil.checkVal(userId,null);
	}

	/**
	 * @return the teamId
	 */
	public String getTeamId() {
		return teamId;
	}

	/**
	 * @param teamId the teamId to set
	 */
	public void setTeamId(String teamId) {
		this.teamId = StringUtil.checkVal(teamId,null);
	}

	/**
	 * @return the accountId
	 */
	public String getAccountId() {
		return accountId;
	}

	/**
	 * @param accountId the accountId to set
	 */
	public void setAccountId(String accountId) {
		this.accountId = StringUtil.checkVal(accountId,null);
	}

}
