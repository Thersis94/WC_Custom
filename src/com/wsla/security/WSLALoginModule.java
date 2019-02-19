package com.wsla.security;

// JDK 1.8.x
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.filter.fileupload.Constants;
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
 * 	10.23.18 - SQL join to the user's first primary location.  Changed overwritten method to support cookie logins.
 ****************************************************************************/
public class WSLALoginModule extends DBLoginModule {

	public WSLALoginModule() {
		super();
	}

	public WSLALoginModule(Map<String, Object> config) {
		super(config);
	}

	/*
	 * Override loadUserData because it's called after both username/password 
	 * logins as well as after cookie logins.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.security.DBLoginModule#loadUserData(java.lang.String, java.lang.String)
	 */
	@Override
	protected UserDataVO loadUserData(String profileId, String authenticationId) {
		UserDataVO user = super.loadUserData(profileId, authenticationId);
		if (user == null) return null; //same logic as superclass

		user.setUserExtendedInfo(getWSLAUser(user.getProfileId()));
		return user;
	}

	/**
	 * Get the user, and the first location they're tied to
	 * @param profileId
	 * @return
	 */
	protected UserVO getWSLAUser(String profileId) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(256);
		sql.append("select u.*, locn.location_id, locn.provider_id, p.provider_type_id from ").append(schema);
		sql.append("wsla_user u ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("WSLA_PROVIDER_USER_XR xr on u.user_id=xr.user_id and xr.active_flg=1 ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("WSLA_PROVIDER_LOCATION locn on xr.location_id=locn.location_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("WSLA_PROVIDER p on locn.provider_id = p.provider_id ");
		sql.append("where u.profile_id=? ");
		sql.append("order by coalesce(xr.primary_contact_flg, -1) desc limit 1"); //favor any location where the user is a primary contact
		log.debug(sql);

		DBProcessor db = new DBProcessor((Connection)getAttribute(GlobalConfig.KEY_DB_CONN), schema);
		List<UserVO> res = db.executeSelect(sql.toString(), Arrays.asList(profileId), new UserVO());
		return !res.isEmpty() ? res.get(0) : null;
	}
}