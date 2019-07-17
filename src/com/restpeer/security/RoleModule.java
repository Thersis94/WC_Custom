package com.restpeer.security;

//JDK 1.8.x
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// RestPeer Custom
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
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.security.UserDataVO;

// WC3
import com.smt.sitebuilder.action.commerce.product.InvoiceAction;
import com.smt.sitebuilder.action.dealer.DealerInfoAction;
import com.smt.sitebuilder.action.dealer.DealerVO;
import com.smt.sitebuilder.admin.action.OrganizationAction;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.DBRoleModule;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title:</b> RoleModule.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Extends core functionality to determine if MobileRest
 * users have a valid payment for using the system.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Jun 27 2019
 * @updates:
 ****************************************************************************/

public class RoleModule extends DBRoleModule {

	public RoleModule() {
		super();
	}

	/**
	 * @param init
	 */
	public RoleModule(Map<String, Object> init) {
		super(init);
	}

	/**
	 * Adds data for MobileRest users to their extended user info, regarding the
	 * status of valid membership payments for using the system.
	 * .
	 */
	@Override
	public SBUserRole getUserRole(String profileId, String siteId) throws AuthorizationException {
		SBUserRole role = super.getUserRole(profileId, siteId);
		UserDataVO user = (UserDataVO) getAttribute(Constants.USER_DATA);
		RPUserVO rpUser = (RPUserVO) user.getUserExtendedInfo();
		
		// Check if the user's dealers have active payments to use the Mobile Restaurateur
		if (RPRole.MEMBER.getRoleId().equals(role.getRoleId())) {
			validatePayments(user.getProfileId(), rpUser);
		}
		
		return role;
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
			
			DealerInfoAction dia = new DealerInfoAction(dbConn, getInitVals());
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
			
			InvoiceAction ia = new InvoiceAction(dbConn, getInitVals());
			List<InvoiceVO> invoices = ia.getLatestByCategory(dealerIds, RPConstants.MEMBERSHIP_CAT);
			for (InvoiceVO invoice : invoices) {
				boolean isPaymentInactive = InvoiceStatus.CANCELED == invoice.getStatusCode() || InvoiceStatus.SUSPENDED == invoice.getStatusCode();
				dealerStatus.put(invoice.getDealerId(), isPaymentInactive ? 0 : 1);
			}
		}

		// Set the status onto the extended user data
		rpUser.setMobileRestStatus(dealerStatus);
	}
}
