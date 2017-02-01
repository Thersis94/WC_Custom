package com.biomed.smarttrak.admin.user;

// Java 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// WC Custom libs
import com.biomed.smarttrak.vo.SmarttrakUserVO;

//SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.exception.InvalidDataException;

//WebCrescendo libs
import com.smt.sitebuilder.action.SBActionAdapter;
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
	
	public UserManagerAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// for admin subsite: retrieve Smarttrak user (WC profile, Smarttrak userId, etc.).
		
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// for admin subsite: will update Smarttrak user data.
	}

	/**
	 * Retrieves SmartTRAK user data from custom table(s).
	 * @param profileId
	 * @return
	 * @throws ActionException
	 */
	public void retrieveBaseUser(SmarttrakUserVO tkUser) 
			throws ActionException {
		StringBuilder sql = new StringBuilder(175);
		sql.append("select user_id, profile_id, account_id, register_submittal_id from ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_user ");
		sql.append("where 1=1 ");
		if (tkUser.getUserId() != null) sql.append("and user_id = ? ");
		if (tkUser.getProfileId() != null) sql.append("and profile_id = ? ");
		if (tkUser.getRegisterSubmittalId() != null) sql.append("and register_submittal_id = ? ");
		log.debug("SmartTRAK user SQL: " + sql.toString());

		StringBuilder errMsg = new StringBuilder(100);
		errMsg.append("Error retrieving SmartTRAK user Id: ");
		int idx = 1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			if (tkUser.getUserId() != null) ps.setString(idx++, tkUser.getUserId());
			if (tkUser.getProfileId() != null) ps.setString(idx++, tkUser.getProfileId());
			if (tkUser.getRegisterSubmittalId() != null) ps.setString(idx++, tkUser.getRegisterSubmittalId());
			DBUtil db = new DBUtil();
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				tkUser.setUserId(db.getStringVal("user_id", rs));
				tkUser.setProfileId(db.getStringVal("profile_id", rs));
				tkUser.setRegisterSubmittalId(db.getStringVal("register_submittal_id", rs));
			} else {
				throw new InvalidDataException("SmartTRAK user does not exist.");
			}
		} catch (SQLException sqle) {
			errMsg.append(sqle.getMessage());
			throw new ActionException(errMsg.toString());
			
		} catch (InvalidDataException ide) {
			errMsg.append(ide.getMessage());
			throw new ActionException(errMsg.toString());

		}
	}
}
