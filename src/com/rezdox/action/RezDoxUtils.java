package com.rezdox.action;

// Java 8
import java.util.Map;

//WC Custom
import com.rezdox.vo.MemberVO;
// SMTBaseLibs
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

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
	public static final String NEW_MEMBER_RESIDENCE_PATH = MEMBER_ROOT_PATH + "/new-residence";
	public static final String ROOMS_PATH = MEMBER_ROOT_PATH + "/rooms";
	public static final String BUSINESS_PATH = MEMBER_ROOT_PATH + "/business";
	public static final String NEW_MEMBER_BUSINESS_PATH = MEMBER_ROOT_PATH + "/new-business";
	public static final String BUSINESS_STOREFRONT_PATH = MEMBER_ROOT_PATH + "/storefront";
	public static final String REVIEW_PATH = MEMBER_ROOT_PATH + "/review";
	public static final String ALBUM_PATH = MEMBER_ROOT_PATH + "/gallery";
	public static final String PHOTO_PATH = MEMBER_ROOT_PATH + "/photo";
	public static final String PROFILE_PATH = MEMBER_ROOT_PATH + "/profile";
	public static final String PROJECT_PATH = MEMBER_ROOT_PATH + "/projects";
	public static final String HOME_HISTORY_PATH = MEMBER_ROOT_PATH + "/history";
	public static final String INVENTORY_PATH = MEMBER_ROOT_PATH + "/inventory";
	public static final String REWARD_PATH = MEMBER_ROOT_PATH + "/rewards";
	public static final String CONNECTION_PATH = MEMBER_ROOT_PATH + "/connections";
	public static final String DIRECTORY_PATH = MEMBER_ROOT_PATH + "/directory";
	public static final String SHARE_PATH = MEMBER_ROOT_PATH + "/share";
	public static final String JOIN_PATH = "/join";
	public static final String EARN_PATH = REWARD_PATH + "/earn";

	/**
	 * coefficient modifier for putting a dollar value on home improvements (projects)
	 */
	public static final double IMPROVEMENTS_VALUE_COEF = .537;

	/**
	 * Org Roles for the site
	 */
	public static final String REZDOX_RESIDENCE_ROLE = "REZDOX_RESIDENCE";
	public static final String REZDOX_RESIDENCE_ROLE_NAME = "RezDox Residence Role";
	public static final int REZDOX_RESIDENCE_ROLE_LEVEL = 25;

	public static final String REZDOX_BUSINESS_ROLE = "REZDOX_BUSINESS";
	public static final String REZDOX_BUSINESS_ROLE_NAME = "RezDox Business Role";
	public static final int REZDOX_BUSINESS_ROLE_LEVEL = 35;

	public static final String REZDOX_RES_BUS_ROLE = "REZDOX_RES_BUS";
	public static final String REZDOX_RES_BUS_ROLE_NAME = "	RezDox Residence and Business Role";
	public static final int REZDOX_RES_BUS_ROLE_LEVEL = 55;
	
	/**
	 * Products are memberships which represent "a residence" or "a business", which we run counts 
	 * against to see if more credits need to be purchased prior to creating new ones.
	 * Note: These are not membership types (aka user roles) - these are purchaseable products in the Store.
	 */
	public enum Product {
		RESIDENCE, BUSINESS
	}

	/**
	 * the pkId of the "100 Connections" we give to each new business upon creation (See BusinessAction)
	 */
	public static final String FREE_CONNECTIONS_PKID = "CONNECTIONS100";
	
	/**
	 * Used for setting notifications from outside of the sub-site context; like in the admintool.
	 */
	public static final String MEMBER_SITE_ID = "REZDOX_1";

	/**
	 * Used for setting running Data Tool reports  in the admintool.
	 */
	public static final String MAIN_SITE_ID = "REZDOX_2";

	/**
	 * The businessId of the RezDox business - used when forming Connections during account creation
	 * Value taken from production, 5/29/19 - https://www.rezdox.com/member/storefront?storeFront=1&businessId=1ddc7800e696d19eac10023edd2c87b7
	 */
	public static final String REZDOX_BUSINESS_ID = "1ddc7800e696d19eac10023edd2c87b7";

	/**
	 * email slugs that correlated to the database/email campaigns.
	 */
	public enum EmailSlug {
		TRANSFER_WAITING, TRANSFER_COMPLETE, BUSINESS_APPROVED, 
		BUSINESS_DECLINED, INVITE_ACCEPTED, REVIEW_BUSINESS, 
		CONNECTION_REQUEST, CONNECTION_REQUEST_FRM_BIZ, CONNECTION_APPROVED, WELCOME, 
		PROJ_ACCPT_BUSINESS, PROJ_ACCPT_HOMEOWNER, 
		PROJ_SHARE_BUSINESS, PROJ_SHARE_HOMEOWNER,
		BUSINESS_SHARED, RESIDENCE_SHARED, //see SharingAction
		BUSINESS_PENDING;
	}

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
		return getFormId(attributes, ModuleVO.ATTRIBUTE_1);
	}


	/**
	 * Get's an alternate form id used by the action
	 * @param attributes
	 * @param mapKey
	 * @return
	 */
	public static String getFormId(Map<String, Object> attributes,  String mapKey) {
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		return (String) mod.getAttribute(mapKey);
	}


	/**
	 * Returns the member from session (UserDataVO)
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
		return getMember(req).getMemberId();
	}

	/**
	 * Checks if the user's role is a residence role
	 * @param role
	 * @return
	 */
	public static boolean isResidenceRole(SBUserRole role) {
		return isResidenceRole(role, true);
	}

	/**
	 * Checks if the user's role is a residence role
	 * Overloaded to omit hybrid users -JM- 05.23.19
	 * @param role
	 * @return
	 */
	public static boolean isResidenceRole(SBUserRole role, boolean orHybrid) {
		return REZDOX_RESIDENCE_ROLE.equals(role.getRoleId()) || (orHybrid && REZDOX_RES_BUS_ROLE.equals(role.getRoleId()));
	}

	/**
	 * Checks if the user's role is a business role
	 * @param role
	 * @return
	 */
	public static boolean isBusinessRole(SBUserRole role) {
		return isBusinessRole(role, true);
	}

	/**
	 * Checks if the user's role is a business role
	 * Overloaded to omit hybrid users -JM- 05.23.19
	 * @param role
	 * @return
	 */
	public static boolean isBusinessRole(SBUserRole role, boolean orHybrid) {
		return REZDOX_BUSINESS_ROLE.equals(role.getRoleId()) || (orHybrid && REZDOX_RES_BUS_ROLE.equals(role.getRoleId()));
	}

	/**
	 * Checks if the user's role is both a business and residence role (hybrid)
	 * @param role
	 * @return
	 */
	public static boolean isHybridRole(SBUserRole role) {
		return REZDOX_RES_BUS_ROLE.equals(role.getRoleId());
	}
}