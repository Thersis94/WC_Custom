package com.biomed.smarttrak.admin.user;

//Java 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// WC Custom libs
import com.biomed.smarttrak.vo.UserVO;

//SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

//WebCrescendo libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: UserManagerAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 1, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserManagerAction extends SBActionAdapter {
	
	/**
	 * Constructor
	 */
	public UserManagerAction() {
		// constructor stub
		super();
	}
	
	/**
	 * Constructor
	 */
	public UserManagerAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		log.debug("mod is: " + (mod == null ? "null" : "not null"));
		Map<String, UserVO> users;
		try {
			users = retrieveCompleteUsers(req);
		} catch (Exception e) {
			throw new ActionException(e.getMessage());
		}
		// put module data
		this.putModuleData(users, users.size(), false, mod.getErrorMessage(), mod.getErrorCondition());
		
	}
	
	/**
	 * Returns a list of complete profiles (i.e. WebCrescendo and SmartTRAK profile data) 
	 * for SmartTRAK users using field values found on the UserVO object that is passed in 
	 * as the argument. If a null argument is passed, all users are returned in the list.
	 * @return
	 * @throws SQLException
	 * @throws DatabaseException
	 */
	protected Map<String, UserVO> retrieveCompleteUsers(ActionRequest req) 
			throws SQLException, DatabaseException {
		log.debug("retrieveCompleteUsers...");
		Map<String, UserVO> baseUsers = retrieveBaseUsers(req);
		Map<String, UserDataVO> profiles = retrieveProfiles(baseUsers);
		if (profiles != null) log.debug("found profiles: " + profiles.size());
		for (Map.Entry<String,UserVO> baseUser : baseUsers.entrySet()) {
			UserDataVO profile = profiles.get(baseUser.getKey());
			if (profile == null) continue;
			baseUser.getValue().setData(profile.getDataMap());
		}
		log.debug("number of complete users returned: " + baseUsers.size());
		return baseUsers;
	}

	/**
	 * Retrieves SmartTRAK-specific user data from custom table(s).
	 * @return
	 * @throws SQLException
	 */
	protected Map<String, UserVO> retrieveBaseUsers(ActionRequest req) throws SQLException {
		log.debug("retrieveBaseUsers...");
		String accountId = StringUtil.checkVal(req.getParameter("accountId"),null);
		StringBuilder sql = formatBaseRetrieveQuery(accountId);
		log.debug("accountId is: " + (accountId ==null ? "null" : "not null"));
		StringBuilder errMsg = new StringBuilder(100);
		errMsg.append("Error retrieving SmartTRAK user(s), account ID: " + accountId);
		
		int idx = 1;
		UserVO user;
		Map<String, UserVO> users = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			if (accountId != null) ps.setString(idx++, accountId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				//user = new UserVO(rs);
				//users.put(user.getProfileId(), user);
			}
			
		} catch (SQLException sqle) {
			errMsg.append(sqle.getMessage());
			throw new SQLException(errMsg.toString());
		}
		log.debug("found base users: " + users.size());
		return users;
	}

	/**
	 * Retrieves a SmartTRAK user's WebCrescendo profile and adds that data to the
	 * user data bean. 
	 * @param users
	 * @return
	 * @throws DatabaseException
	 */
	public Map<String, UserDataVO> retrieveProfiles(Map<String, UserVO> users) 
			throws DatabaseException {
		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		List<String> profileIds = new ArrayList<>();
		for (Map.Entry<String, UserVO> entry : users.entrySet()) {
			profileIds.add(entry.getValue().getProfileId());
		}
		return pm.searchProfileMap(dbConn, profileIds);
	}

	/**
	 * Formats the base user retrieve query statement.
	 * @return
	 */
	private StringBuilder formatBaseRetrieveQuery(String accountId) {
		String schema = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		if (accountId == null) {
			// account owners only
			return formatAccountOwnersQuery(schema);
		} else {
			// all other cases, we will need all account users.
			return formatAccountUsersQuery(schema);
		}
	}
	
	/**
	 * Builds the query to retrieve account owners.
	 * @param schema
	 * @return
	 */
	private StringBuilder formatAccountOwnersQuery(String schema) {
		log.debug("building query to retrieve accounts owners only...");
		StringBuilder sql = new StringBuilder(100);
		sql.append("select distinct(owner_profile_id) as profile_id ");
		sql.append("from ").append(schema).append("biomedgps_account ");
		sql.append("where owner_profile_id is not null");
		log.debug("SmartTRAK retrieve account owners SQL: " + sql.toString());
		return sql;
	}
	
	/**
	 * Builds the query to retrieve an account's users.
	 * @param schema
	 * @param profileId
	 * @return
	 */
	private StringBuilder formatAccountUsersQuery(String schema) {
		log.debug("building query to retrieve account users...");
		StringBuilder sql = new StringBuilder(260);
		sql.append("select a.account_id, u.user_id, u.profile_id, u.register_submittal_id, ");
		sql.append("u.create_dt, u.update_dt from custom.biomedgps_account a "); 
		sql.append("inner join custom.biomedgps_user u on a.account_id = u.account_id ");
		sql.append("where a.account_id = ? order by a.account_id");
		log.debug("SmartTRAK retrieve account users SQL: " + sql.toString());
		return sql;
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// for admin subsite: will update Smarttrak user data.
	}
	
}
