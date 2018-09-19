package com.wsla.security;

// JDK 1.8.x
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.security.DBLoginModule;

// WC Custom Libs
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: WSLALoginModule.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Custom DB Login module for WSLA that stores the uservo
 * from the custom project
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 18, 2018
 * @updates:
 ****************************************************************************/

public class WSLALoginModule extends DBLoginModule {

	/**
	 * 
	 */
	public WSLALoginModule() {
		super();
	}

	/**
	 * @param config
	 */
	public WSLALoginModule(Map<String, Object> config) {
		super(config);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.security.DBLoginModule#authenticateUser(java.lang.String, java.lang.String)
	 */
	@Override
	public UserDataVO authenticateUser(String user, String pwd) throws AuthenticationException {
		UserDataVO userData = super.authenticateUser(user, pwd);
		userData.setUserExtendedInfo(getWSLAUser(userData.getProfileId()));
		
		return userData;
	}
	
	/**
	 * Retrieves the user from the WSLA tables
	 * @param profileId
	 * @return
	 */
	protected UserVO getWSLAUser(String profileId) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(this.getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("wsla_user where profile_id = ?");
		
		DBProcessor db = new DBProcessor((Connection)getAttribute(GlobalConfig.KEY_DB_CONN));
		List<UserVO> res = db.executeSelect(sql.toString(), Arrays.asList(profileId), new UserVO());
		
		return res.isEmpty() ? null : res.get(0);
	}
}

