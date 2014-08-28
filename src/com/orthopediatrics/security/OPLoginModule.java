package com.orthopediatrics.security;

// JDK 6.x
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

// SB_Orthopediatrics lib
import com.orthopediatrics.action.SalesRepVO;

// SMTBaseLibs
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;

// WC 2.0 libs
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.security.DBLoginModule;

/****************************************************************************
 * <b>Title</b>: OPLoginModule.java <p/>
 * <b>Project</b>: SB_Orthopediatrics <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jul 23, 2011<p/>
 * <b>Changes:
 * Jan 10, 2013 - DBargerhuff; added method to retrieve user data based on an encrypted profile ID.
 * </b>
 ****************************************************************************/
public class OPLoginModule extends DBLoginModule {

	/**
	 * 
	 */
	public OPLoginModule() {
		this.setUserProfile(true);
	}

	/**
	 * @param arg0
	 */
	public OPLoginModule(Map<String, Object> arg0) {
		super(arg0);
		this.setUserProfile(true);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#retrieveUserData(java.lang.String, java.lang.String)
	 */
	public UserDataVO retrieveUserData(String loginName, String password) 
			throws AuthenticationException {
		UserDataVO authUser = this.authenticateUser(loginName, password);
		UserDataVO user = new UserDataVO();
		user.setAuthenticationId(authUser.getAuthenticationId());
		user.setPasswordResetFlag(authUser.getPasswordResetFlag());
		user.setPasswordHistory(authUser.getPasswordHistory());
		if (authUser.isAuthenticated()) {
			// Retrieve the profile
		    ProfileManager pm = ProfileManagerFactory.getInstance(this.initVals);
	        Connection dbConn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
	        try {
	        	user = pm.getProfile(user.getAuthenticationId(), dbConn, ProfileManager.AUTH_ID_LOOKUP, null);
	        	 // Retrieve the User's Extended Information
	        	user.setUserExtendedInfo(this.getRepInfo(user.getProfileId(), dbConn));
	        	user.setAuthenticated(true);
	        	log.debug("retrieved user profile");
	        } catch(Exception e) {
	        	log.error("Unable to retrieve profile: " + e.getMessage());
	        }
		}
		return user;
	}
	
	/**
	 * Retrieves user data based on the encrypted profileId passed in.
	 * @param encProfileId
	 */
	public UserDataVO retrieveUserData(String encProfileId) throws AuthenticationException {
		UserDataVO user =  super.retrieveUserData(encProfileId);
		if (user != null) {
			// Retrieve the User's Extended Information
			Connection dbConn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
			try {
				user.setUserExtendedInfo(this.getRepInfo(user.getProfileId(), dbConn));
			} catch (SQLException sqle) {
				log.error("Error retrieving user extended data, ", sqle);
			}
		}
		return user;
	}
	
	/**
	 * 
	 * @param profileId
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public SalesRepVO getRepInfo(String profileId, Connection conn) throws SQLException {
		log.debug("retrieving sales rep info...");
		String dbs = (String)initVals.get("customDbSchema");
		SalesRepVO rep = null;
		StringBuilder s = new StringBuilder();
		s.append("select * from ").append(dbs).append("op_sales_rep a ");
		s.append("where profile_id = ? ");
		log.debug("Sales Rep Info on login: " + s + "|" + profileId);
		
		PreparedStatement ps = conn.prepareStatement(s.toString());
		ps.setString(1, profileId);
		
		ResultSet rs = ps.executeQuery();
		if (rs.next()) rep = new SalesRepVO(rs);
		
		return rep;
	}
}
