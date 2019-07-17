package com.restpeer.action.account;

// JDK 1.8.x
import java.util.Map;

// RP Libs
import com.restpeer.common.RPConstants.RPRole;
import com.restpeer.data.RPUserVO;
import com.restpeer.action.admin.UserAction;
import com.restpeer.common.RPConstants.MemberType;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.InvalidDataException;

// WC Libs
import com.smt.sitebuilder.action.dealer.DealerLocationProfileAction;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: MemberLocationUserAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the assignment of Users to a location
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 19, 2019
 * @updates:
 ****************************************************************************/

public class MemberLocationUserAction extends DealerLocationProfileAction {
	/**
	 * Json key to access this action
	 */
	public static final String AJAX_KEY = "locationUser";
	
	/**
	 * 
	 */
	public MemberLocationUserAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public MemberLocationUserAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public MemberLocationUserAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		setDBConnection(dbConn);
		setAttributes(attributes);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.user.UserBaseWidget#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (req.hasParameter("profileId")) {
			setModuleData(getUser(req.getParameter("profileId")));
		} else {
			super.retrieve(req);
		}
	}

	/**
	 * 
	 * @param profileId
	 * @return
	 */
	public RPUserVO getUser(String profileId) {
		UserAction uw = new UserAction(getDBConnection(), getAttributes());
		return uw.getUserByProfileId(profileId);
	}
	
	/**
	 * Gets the role. This is used when assigning a user to a location outside
	 * of the admin screens.
	 * @param req
	 * @return
	 * @throws InvalidDataException
	 */
	@Override
	public String getRoleId(ActionRequest req) throws InvalidDataException {
		MemberType mt = null;
		for (MemberType memberType : MemberType.values()) {
			if (memberType.getDealerId() == req.getIntegerParameter("dealerTypeId")) {
				mt = memberType;
				break;
			}
		}
		if (mt == null) throw new InvalidDataException("Invalid Dealer Type Submitted");
		
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		switch (mt) {
			case KITCHEN:
				return RPRole.KITCHEN.getRoleId();
			case CUSTOMER:
				return RPRole.MEMBER.getRoleId();
			case RESTAURANT_PEER:
				if (! role.getRoleId().equals(RPRole.ADMIN.getRoleId()))
					throw new InvalidDataException("Must be admin to add a Restaurant Peer Admin");
				
				return RPRole.ADMIN.getRoleId();
			default:
				return "0";
		}
	}
}

