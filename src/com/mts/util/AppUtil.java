package com.mts.util;

import com.mts.subscriber.data.MTSUserVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: AppUtil.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Utility Helper Class
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jul 26, 2019
 * @updates:
 ****************************************************************************/
public class AppUtil {
	
	private AppUtil() {
		//static class
	}
	
	/**
	 * Gets the MTS user info from the session
	 * @param req
	 * @return
	 */
	public static String getMTSUserId(ActionRequest req) {
		String userId = "";
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		if (user != null) {
			MTSUserVO mUser = (MTSUserVO)user.getUserExtendedInfo();
			if (mUser != null) userId = mUser.getUserId();
		}
		
		return userId;
	}
	
	/**
	 * Retrieves the MTS user data from the session
	 * @param req
	 * @return
	 */
	public static MTSUserVO getMTSUser(ActionRequest req) {
		MTSUserVO mUser = new MTSUserVO();
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		
		if (user != null) {
			mUser = (MTSUserVO)user.getUserExtendedInfo();
		}
		
		return mUser;
	}
}
