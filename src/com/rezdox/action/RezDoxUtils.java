package com.rezdox.action;

// JDK 1.8.x
import java.util.Map;

// App Libs
import com.rezdox.vo.MemberVO;

// SMT Base libs 3.5
import com.siliconmtn.action.ActionRequest;

// WC Libs 3.8
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> RezDoxUtils.java<br/>
 * <b>Description:</b> Utility class for defining constants and core info for the rezdox member portal
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
	 * More available rewards bound to certain activities in the system
	 */
	public static final String REWARD_CREATE_BUSINESS = "CREATE_BUS";
	public static final String REWARD_BUSINESS_INVITE = "INVITE_BUS";
	public static final String REWARD_HOMEOWNER_INVITE = "INVITE_MEM";

	/**
	 * Root path for the member portal site
	 */
	public static final String MEMBER_ROOT_PATH = "/member";

	/**
	 * Path to redirect to when the user is required to subscribe before they can
	 * continue with the action they were trying to take.
	 */
	public static final String SUBSCRIPTION_UPGRADE_PATH = MEMBER_ROOT_PATH + "/store";
	
	/**
	 * Paths to various parts of the site
	 * WARNING: Only use these in the case where one action's view has links to a separate page/action
	 */
	public static final String RESIDENCE_PATH = MEMBER_ROOT_PATH + "/residence";
	public static final String ROOMS_PATH = MEMBER_ROOT_PATH + "/rooms";
	public static final String BUSINESS_PATH = MEMBER_ROOT_PATH + "/business";
	public static final String REVIEW_PATH = MEMBER_ROOT_PATH + "/review";
	public static final String ALBUM_PATH = MEMBER_ROOT_PATH + "/gallery";
	public static final String PHOTO_PATH = MEMBER_ROOT_PATH + "/photo";
	public static final String PROFILE_PATH = MEMBER_ROOT_PATH + "/profile";
	public static final String PROJECT_PATH = MEMBER_ROOT_PATH + "/projects";
	public static final String REWARD_PATH = MEMBER_ROOT_PATH + "/rewards";
	public static final String CONNECTION_PATH = MEMBER_ROOT_PATH + "/Connections";

	/**
	 * coefficient modifier for putting a dollar value on home improvements (projects)
	 */
	public static final double IMPROVEMENTS_VALUE_COEF = .537;
	
	/**
	 * Org Roles for the site
	 */
	public static final String REZDOX_RESIDENCE_ROLE = "REZDOX_RESIDENCE";
	public static final String REZDOX_BUSINESS_ROLE = "REZDOX_BUSINESS";
	public static final String REZDOX_RES_BUS_ROLE = "REZDOX_RES_BUS";

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
	 * Returns the member from session (UserDataVO)
	 * 
	 * @param req
	 * @return
	 */
	public static MemberVO getMember(ActionRequest req) {
		return (MemberVO) req.getSession().getAttribute(Constants.USER_DATA);
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


	/**
	 * Returns the businessId from session (UserDataVO)
	 * @param req
	 * @return
	 */
	public static String getBusinessId(ActionRequest req) {
		MemberVO member = (MemberVO) req.getSession().getAttribute(Constants.USER_DATA);
		return member.getBusinessId();
	}
}