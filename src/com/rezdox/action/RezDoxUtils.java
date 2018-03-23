package com.rezdox.action;

import java.util.Map;

import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> RezDoxUtils.java<br/>
 * <b>Description:</b> 
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 24, 2018
 ****************************************************************************/
public class RezDoxUtils {

	/**
	 * This is the rewardId we give to new homeowners when they first sign-up.
	 * The site admin can change which reward "SIGNUP" actually binds to in the WC admintool.
	 */
	public static final String NEW_REGISTRANT_REWARD = "SIGNUP";

	/**
	 * Root path for the member portal site
	 */
	public static final String MEMBER_ROOT_PATH = "/member";

	/**
	 * Path to redirect to when the user is required to subscribe before they can
	 * continue with the action they were trying to take.
	 */
	public static final String SUBSCRIPTION_UPGRADE_PATH = MEMBER_ROOT_PATH + "/subscribe";
	
	/**
	 * Paths to various parts of the site
	 * WARNING: Only use these in the case where one action's view has links to a separate page/action
	 * TODO: Add these to their respective places in the jsp files 
	 */
	public static final String RESIDENCE_PATH = MEMBER_ROOT_PATH + "/residence";
	public static final String ROOMS_PATH = MEMBER_ROOT_PATH + "/rooms";
	public static final String BUSINESS_PATH = MEMBER_ROOT_PATH + "/business";
	public static final String ALBUM_PATH = MEMBER_ROOT_PATH + "/gallery";
	public static final String PHOTO_PATH = MEMBER_ROOT_PATH + "/photo";

	private RezDoxUtils() {
		//default constructor not used in static classes
	}


	/**
	 * Get's the form id associated to the action off the attributes Map.
	 * The RezDox actions store this in the attribute1 slot.
	 * @param attributes
	 * @return
	 */
	public static String getFormId(Map<String, Object> attributes) {
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		return (String) mod.getAttribute(ModuleVO.ATTRIBUTE_1);
	}


	/**
	 * Returns the memberId from session (UserDataVO)
	 * @param req
	 * @return
	 */
	public static String getMemberId(ActionRequest req) {
		MemberVO member = (MemberVO) req.getSession().getAttribute(Constants.USER_DATA);
		return member.getMemberId();
	}
}