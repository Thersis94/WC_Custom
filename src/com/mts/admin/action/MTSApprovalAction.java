package com.mts.admin.action;

// MTS Libs
import com.mts.common.MTSConstants.MTSRole;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.approval.ApprovalAction;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: MTSApprovalAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Front-end process to manage the approvals of the MTS documents
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 30, 2019
 * @updates:
 ****************************************************************************/

public class MTSApprovalAction extends SBActionAdapter {

	/**
	 * Ajax Controller key for this action
	 */
	public static final String AJAX_KEY = "approval";
	
	/**
	 * 
	 */
	public MTSApprovalAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public MTSApprovalAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// Make sure the user is authorized to approve the submitted item
		SBUserRole role = (SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA);
		if (! role.hasMatchingRole(MTSRole.ADMIN.getRoleId())) {
			setModuleData(null, 0, "Not Authorized to approve");
			return;
		}

		try {
			// Call the approval Action
			ApprovalAction ac = new ApprovalAction(getDBConnection(), getAttributes());
			ac.update(req);
		} catch (Exception e) {
			log.error("Error performing an approval", e);
			setModuleData(null, 0, e.getLocalizedMessage());
		}
		
		//Remove the redirects form the admin actions and return the data
		req.removeAttribute(Constants.REDIRECT_REQUEST);
		req.removeAttribute(Constants.REDIRECT_URL);
	}
}

