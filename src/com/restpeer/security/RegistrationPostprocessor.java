package com.restpeer.security;

import java.util.Map;

import com.restpeer.action.admin.UserAction;
import com.restpeer.common.RPConstants;
import com.restpeer.common.RPConstants.MemberType;
import com.restpeer.data.RPUserVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.action.dealer.DealerLocatorAction;
import com.smt.sitebuilder.action.dealer.DealerVO;
import com.smt.sitebuilder.action.dealer.EcommDealerAction;
import com.smt.sitebuilder.action.dealer.EcommDealerLocationAction;
import com.smt.sitebuilder.admin.action.OrganizationAction;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> RegistrationPostprocessor.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Jun 06 2019
 * @updates:
 ****************************************************************************/

public class RegistrationPostprocessor extends SBActionAdapter {
	
	private MemberType memberType;

	public RegistrationPostprocessor() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public RegistrationPostprocessor(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public RegistrationPostprocessor(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		setDBConnection(dbConn);
		setAttributes(attributes);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		super.build(req);
		
		createDealer(req);
		createDealerLocation(req);
		createUser(req);
		
		req.setParameter(Constants.REDIRECT_URL, RPConstants.PORTAL_PATH);
	}

	/**
	 * Creates the user's dealer record from the submittal data.
	 * 
	 * @param req
	 * @return
	 * @throws ActionException 
	 */
	protected DealerVO createDealer(ActionRequest req) throws ActionException {
		if (memberType == null) {
			memberType = MemberType.KITCHEN;
		}

		req.setParameter("insertAction", "true");
		req.setParameter("dealerName", req.getParameter("reg_||b801a9892edaeb7d7f000101c36ff1dc"));
		req.setParameter("dealerTypeId", Integer.toString(memberType.getDealerId()));
		req.setParameter(OrganizationAction.ORGANIZATION_ID, ((SiteVO) req.getAttribute(Constants.SITE_DATA)).getOrganizationId());
		req.setParameter("activeFlag", MemberType.KITCHEN == memberType ? "1" : "0"); // Kitchens are free to list, MobileRest will be activated after payment
		
		EcommDealerAction eda = new EcommDealerAction(getDBConnection(), getAttributes());
		eda.build(req);
		
		return new DealerVO(req);
	}

	/**
	 * Creates the user's initial dealer location record from the submittal data.
	 * 
	 * @param req
	 * @return
	 * @throws ActionException 
	 */
	protected DealerLocationVO createDealerLocation(ActionRequest req) throws ActionException {
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		
		req.setParameter(DealerLocatorAction.DEALER_LOCATION_ID, new UUIDGenerator().getUUID());
		req.setParameter("locationName", StringUtil.join(req.getParameter("reg_||b801a9892edaeb7d7f000101c36ff1dc"), " - ", user.getCity()));
		req.setParameter("address", user.getAddress());
		req.setParameter("address2", user.getAddress2());
		req.setParameter("city", user.getCity());
		req.setParameter("state", user.getState());
		req.setParameter("zip", user.getZipCode());
		req.setParameter("country", user.getCountryCode());
		
		EcommDealerLocationAction edla = new EcommDealerLocationAction(getDBConnection(), getAttributes());
		edla.build(req);
		
		return new DealerLocationVO(req);
	}
	
	/**
	 * Creates a new user from the submittal data.
	 * 
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected RPUserVO createUser(ActionRequest req) throws ActionException {
		RPUserVO user = new RPUserVO();
		user.setProfile((UserDataVO) req.getSession().getAttribute(Constants.USER_DATA));
		user.setProfileId(user.getProfile().getProfileId());
		user.setFirstName(user.getProfile().getFirstName());
		user.setLastName(user.getProfile().getLastName());
		user.setEmailAddress(user.getProfile().getEmailAddress());
		user.setPhoneNumber(user.getProfile().getMainPhone());
		
		req.setParameter("isInsert", "true");
		UserAction ua = new UserAction(getDBConnection(), getAttributes());
		ua.saveCustomUser(user, req);
		
		return user;
	}

	/**
	 * @param memberType the memberType to set
	 */
	public void setMemberType(MemberType memberType) {
		this.memberType = memberType;
	}
}
