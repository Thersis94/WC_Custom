package com.biomed.smarttrak.security;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SAMLLoginModule;

/****************************************************************************
 * Title: SmarttrakSSOLoginModule.java <p/>
 * Project: WC_Custom <p/>
 * Description: Login module for smarttrak SSO that loads the smarttrak user information
 * for a SSO login if it exists and creates it if it doesn't.<p/>
 * Copyright: Copyright (c) 2019<p/>
 * Company: Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Jan 8, 2019
 ****************************************************************************/

public class SmarttrakSSOLoginModule extends SAMLLoginModule {
	
	private Connection dbConn = null;

	public SmarttrakSSOLoginModule() {
		this(new HashMap<String, Object>());
	}

	public SmarttrakSSOLoginModule(Map<String, Object> config) {
		super(config);
		dbConn = (Connection)getAttribute(GlobalConfig.KEY_DB_CONN);
	}
	
	
	@Override
	public UserDataVO authenticateUser(String user, String pwd) throws AuthenticationException {
		UserDataVO userData = super.authenticateUser(user, pwd);
		
		return loadSmarttrakUser(userData);
	}
	

	@Override
	public UserDataVO authenticateUser(String encProfileId) throws AuthenticationException {
		UserDataVO userData = super.authenticateUser(encProfileId);
		
		return loadSmarttrakUser(userData);
	}

	
	private UserDataVO loadSmarttrakUser(UserDataVO userData) {
		SmartTRAKLoginModule login = new SmartTRAKLoginModule(getAttributes());
		UserVO user = login.loadSmarttrakUser(userData);
		
		if (StringUtil.isEmpty(user.getUserId())) {
			user.setAccountId(loadAccount());
			user.setAuthenticated(true);
			createUser(user);
		}
		
		return user;
	}
	
	private String loadAccount() {
		StringBuilder sql = new StringBuilder(100);
		sql.append("select account_id from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_account where account_id = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, (String) getAttribute("accountId"));
			
			ResultSet rs = ps.executeQuery();
			
			if(rs.next())
				return rs.getString("account_id");
		} catch (SQLException e) {
			log.error("Failed to get account for SSO user", e);
		}
		return "";
	}

	
	private void createUser(UserVO user) {
		DBProcessor db = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		
		try {
			db.save(user);
		} catch (Exception e) {
			log.error("Failed to create user for account " + user.getAccountId(), e);
		}
	}

}
