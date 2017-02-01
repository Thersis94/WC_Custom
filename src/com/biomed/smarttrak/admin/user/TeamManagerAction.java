package com.biomed.smarttrak.admin.user;

// Java 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

//WC_Custom libs
import com.biomed.smarttrak.vo.SmarttrakTeamVO;
import com.biomed.smarttrak.vo.SmarttrakUserVO;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;

// WebCrescendo libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: TeamManagerAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author groot
 @version 1.0
 @since Feb 1, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class TeamManagerAction extends SBActionAdapter {

	/**
	* Constructor
	*/
	public TeamManagerAction() {
		super();
	}
	
	public TeamManagerAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// admin: retrieve team(s)
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// admin: update team(s)
	}
	
	/**
	 * Retrieves user's teams and populates List on user bean.
	 * @param tkUser
	 * @throws ActionException
	 */
	public void retrieveUserTeams(SmarttrakUserVO tkUser) 
			throws ActionException {
		String schema = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(325);
		sql.append("select a.user_id, a.profile_id, a.account_id, b.team_id, c.team_nm from ");
		sql.append(schema).append("biomedgps_user a ");
		sql.append("inner join ").append(schema).append("biomedgps_user_team_xr b ");
		
		if (tkUser.getUserId() != null) sql.append("and user_id = ? ");
		if (tkUser.getProfileId() != null) sql.append("and profile_id = ? ");
		
		sql.append("inner join ").append(schema).append("biomedgps_team c ");
		sql.append("on b.team_id = c.team_id order by c.team_nm");
		log.debug("SmartTRAK user teams SQL: " + sql.toString());

		StringBuilder errMsg = new StringBuilder(100);
		errMsg.append("Error retrieving SmartTRAK user Id: ");
		int idx = 1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			if (tkUser.getUserId() != null) ps.setString(idx++, tkUser.getUserId());
			if (tkUser.getProfileId() != null) ps.setString(idx++, tkUser.getProfileId());
			ResultSet rs = ps.executeQuery();
			if (rs == null) return;
			DBUtil db = new DBUtil();
			SmarttrakTeamVO team;
			while (rs.next()) {
				team = new SmarttrakTeamVO();
				team.setAccountId(db.getStringVal("account_id", rs));
				team.setTeamId(db.getStringVal("team_id", rs));
				team.setTeamName(db.getStringVal("team_nm", rs));
				tkUser.addTeam(team);
			}
		} catch (SQLException sqle) {
			errMsg.append(sqle.getMessage());
			throw new ActionException(errMsg.toString());
		} 
	}
}
