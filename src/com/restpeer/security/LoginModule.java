package com.restpeer.security;

// JDK 1.8.x
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// RestPeer Custom
import com.restpeer.action.admin.UserAction;
import com.restpeer.common.RPConstants;
import com.restpeer.common.RPConstants.RPRole;
import com.restpeer.data.RPUserVO;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.commerce.catalog.InvoiceVO;
import com.siliconmtn.commerce.catalog.InvoiceVO.InvoiceStatus;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
// WC3
import com.smt.sitebuilder.action.commerce.product.InvoiceAction;
import com.smt.sitebuilder.action.dealer.DealerInfoAction;
import com.smt.sitebuilder.action.dealer.DealerVO;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.admin.action.OrganizationAction;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.security.DBLoginModule;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: LoginModule.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Custom DB Login module for RestPeer that stores the
 * RPUserVO from the custom project and determines if MobileRest users have a
 * valid payment to be using the system.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since June 13, 2019
 ****************************************************************************/
public class LoginModule extends DBLoginModule {

	public LoginModule() {
		super();
	}

	public LoginModule(Map<String, Object> config) {
		super(config);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.security.DBLoginModule#loadUserData(java.lang.String, java.lang.String)
	 */
	@Override
	protected UserDataVO loadUserData(String profileId, String authenticationId) {
		UserDataVO user = super.loadUserData(profileId, authenticationId);
		if (user == null) return null; //same logic as superclass
		
		// Get the extended user data
		RPUserVO rpUser = getRPUser(user.getProfileId());
		if (rpUser.getUserId() == null) return user;
		user.setUserExtendedInfo(rpUser);
		
		// Check if the user's dealers have active payments to use the Mobile Restaurateur
		SBUserRole role = getUserRole(user.getProfileId());
		if (RPRole.MEMBER.getRoleId().equals(role.getRoleId())) {
			validatePayments(user.getProfileId(), rpUser);
		}
		
		return user;
	}
	
	/**
	 * Validates the user's dealers have active payments for
	 * using the Mobile Restaurateur.
	 * 
	 * @param profileId
	 * @param rpUser
	 */
	private void validatePayments(String profileId, RPUserVO rpUser) {
		SMTDBConnection dbConn = (SMTDBConnection) getAttribute(GlobalConfig.KEY_DB_CONN);
		SiteVO site = (SiteVO) getAttribute(Constants.SITE_DATA);
		
		// Get dealers associated to this user
		List<DealerVO> dealers;
		try {
			ActionRequest req = new ActionRequest();
			req.setParameter("srchProfileId", profileId);
			req.setParameter(OrganizationAction.ORGANIZATION_ID, site.getOrganizationId());
			
			DealerInfoAction dia = new DealerInfoAction(dbConn, getAttributes());
			dealers = new ArrayList<>(dia.search(req).values());
		} catch (DatabaseException e) {
			log.error("Unable to get user's dealers", e);
			dealers = new ArrayList<>();
		}
		
		// Validate the status of this user's dealers
		Map<String, Integer> dealerStatus = new HashMap<>();
		if (dealers != null && !dealers.isEmpty()) {
			List<String> dealerIds = new ArrayList<>();
			for (DealerVO dealer : dealers) {
				dealerIds.add(dealer.getDealerId());
			}
			
			InvoiceAction ia = new InvoiceAction(dbConn, getAttributes());
			List<InvoiceVO> invoices = ia.getLatestByCategory(dealerIds, RPConstants.MEMBERSHIP_CAT);
			for (InvoiceVO invoice : invoices) {
				boolean isPaymentInactive = InvoiceStatus.CANCELED == invoice.getStatusCode() || InvoiceStatus.SUSPENDED == invoice.getStatusCode();
				dealerStatus.put(invoice.getDealerId(), isPaymentInactive ? 0 : 1);
			}
			
		}

		// Set the status onto the extended user data
		rpUser.setMobileRestStatus(dealerStatus);
	}
	
	/**
	 * Get's the role associated to this user for the site.
	 * 
	 * @param profileId
	 * @return
	 */
	private SBUserRole getUserRole(String profileId) {
		SiteVO site = (SiteVO) getAttribute(Constants.SITE_DATA);
		Connection dbConn = (Connection) getAttribute(GlobalConfig.KEY_DB_CONN);
		
		SBUserRole role;
		try {
			role = new ProfileRoleManager().getRole(profileId, StringUtil.checkVal(site.getAliasPathParentId(), site.getSiteId()), null, null, dbConn);
		} catch (DatabaseException e) {
			log.error("Unable to get user's role", e);
			role = new SBUserRole();
		}
		
		return role;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.security.DBLoginModule#authenticateUser(java.lang.String, java.lang.String)
	 */
	@Override
	public UserDataVO authenticateUser(String user, String pwd) throws AuthenticationException {
		UserDataVO uvo = super.authenticateUser(user, pwd);
		
		// Catch the edge case of a user in WC3, but not a registered user in RestPeer
		if (uvo.getUserExtendedInfo() == null)
			throw new AuthenticationException("User is a user of the site, but not registered as a RestPeer user");
		
		return uvo;
	}

	/**
	 * Gets the RestPeer user data.
	 * 
	 * @param profileId
	 * @return
	 */
	protected RPUserVO getRPUser(String profileId) {
		SMTDBConnection dbConn = (SMTDBConnection) getAttribute(GlobalConfig.KEY_DB_CONN);
		
		UserAction ua = new UserAction(dbConn, getAttributes());
		return ua.getUserByProfileId(profileId);
	}
}