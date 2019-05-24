package com.wsla.util;

import java.util.Arrays;
import java.util.List;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.task.TaskEmailer;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <p><b>Title:</b> TaskEmailer.java</p>
 * <p><b>Description:</b> overloads methods to load user data from the wsla_member table.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since May 20, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class WSLATaskEmailer extends TaskEmailer {

	@Override
	protected UserDataVO loadUser(String profileId, String orgId) {
		UserDataVO user = super.loadUser(profileId, orgId);

		//also load the wsla_member, which contains their locale
		if (user != null)
			user.setUserExtendedInfo(this.getWSLAUser(user.getProfileId()));

		return user;
	}


	/**
	 * load the member VO, which contains locale
	 * @param profileId
	 * @return
	 */
	protected UserVO getWSLAUser(String profileId) {
		String schema = getCustomSchema();
		String sql =StringUtil.join(DBUtil.SELECT_FROM_STAR, schema, "wsla_user u where profile_id=?");
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<UserVO> res = db.executeSelect(sql, Arrays.asList(profileId), new UserVO());
		return !res.isEmpty() ? res.get(0) : null;
	}
}
