package com.biomed.smarttrak.admin.user;

// Java 7
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Log4j
import org.apache.log4j.Logger;

// WC_Custom
import com.biomed.smarttrak.vo.UserVO;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;

// WebCrescendo
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: UserManager.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 2, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserManager extends AbstractManager {

	private Logger log = Logger.getLogger(UserManager.class);
	private String accountId;
	private String userId;
	private String profileId;
	private String registerSubmittalId;
	
	/**
	* Constructor
	*/
	public UserManager() {
		// constructor stub
	}
	
	public UserManager(Connection dbConn) {
		setDbConn(dbConn);
		setAttributes(new HashMap<String,Object>());
	}
	
	public UserManager(Connection dbConn, Map<String,Object> attributes) {
		this(dbConn);
		setAttributes(attributes);
	}
	
	/**
	 * Retrieves SmartTRAK-specific user data from custom table(s).
	 * @param searchParams
	 * @return
	 * @throws ActionException
	 */
	public List<UserVO> retrieveBaseUser() throws SQLException {
		StringBuilder sql = formatBaseRetrieveQuery();
		if (accountId != null) sql.append("and account_id = ? ");
		if (userId != null) sql.append("and user_id = ? ");
		if (profileId != null) sql.append("and profile_id = ? ");
		if (registerSubmittalId != null) sql.append("and register_submittal_id = ? ");

		log.debug("SmartTRAK retrieve user(s) SQL: " + sql.toString());

		StringBuilder errMsg = new StringBuilder(100);
		errMsg.append("Error retrieving SmartTRAK user Id: ");
		int idx = 1;
		UserVO user;
		List<UserVO> users = new ArrayList<>();
		try (PreparedStatement ps = getDbConn().prepareStatement(sql.toString())) {
			if (accountId != null) ps.setString(idx++, accountId);
			if (userId != null) ps.setString(idx++, userId);
			if (profileId != null) ps.setString(idx++, profileId);
			if (registerSubmittalId != null) ps.setString(idx, registerSubmittalId);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				user = new UserVO(rs);
				users.add(user);
			}
			
		} catch (SQLException sqle) {
			errMsg.append(sqle.getMessage());
			throw new SQLException(errMsg.toString());
		}
		
		return users;
	}
	
	/**
	 * Returns a list of complete profiles (i.e. WebCrescendo and SmartTRAK profile data) 
	 * for SmartTRAK users using field values found on the UserVO object that is passed in 
	 * as the argument. If a null argument is passed, all users are returned in the list.
	 * @param searchParams
	 * @throws SQLException 
	 * @throws DatabaseException 
	 */
	public List<UserVO> retrieveCompleteUser() 
			throws SQLException, DatabaseException {
		List<UserVO> users = retrieveBaseUser();
		Map<String, UserDataVO> profiles = retrieveProfiles(users);
		
		for (UserVO user : users) {
			UserDataVO profile = profiles.get(user.getProfileId());
			if (profile == null) continue;
			user.setData(profile.getDataMap());
		}

		// sort by profile name if appropriate
		if (users.size() > 1) Collections.sort(users, new UserNameComparator());
		return users;
	}
	
	/**
	 * Retrieves a SmartTRAK user's WebCrescendo profile and adds that data to the
	 * user data bean. 
	 * @param users
	 * @return
	 * @throws DatabaseException
	 */
	private Map<String, UserDataVO> retrieveProfiles(List<UserVO> users) 
			throws DatabaseException {
		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		List<String> profileIds = new ArrayList<>();
		for (UserVO user : users) {
			profileIds.add(user.getProfileId());
		}
		return pm.searchProfileMap(getDbConn(), profileIds);
	}
	
	/**
	 * Formats the base user retrieve query statement.
	 * @return
	 */
	private StringBuilder formatBaseRetrieveQuery() {
		StringBuilder sql = new StringBuilder(175);
		sql.append("select user_id, profile_id, account_id, register_submittal_id from ");
		sql.append(getAttributes().get(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_user ");
		sql.append("where 1=1 ");
		return sql;
	}

	/**
	 * @param accountId the accountId to set
	 */
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @param profileId the profileId to set
	 */
	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	/**
	 * @param registerSubmittalId the registerSubmittalId to set
	 */
	public void setRegisterSubmittalId(String registerSubmittalId) {
		this.registerSubmittalId = registerSubmittalId;
	}

}
