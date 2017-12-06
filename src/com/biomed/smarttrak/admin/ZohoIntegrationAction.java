package com.biomed.smarttrak.admin;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.ticketing.SupportUtilFactory;
import com.smt.sitebuilder.action.ticketing.TicketFacadeAction;
import com.smt.sitebuilder.action.ticketing.vo.SupportFieldsVO;

/****************************************************************************
 * <b>Title</b>: ZohoIntegrationAction.java
 * <b>Project</b>: WC Custom
 * <b>Description: </b> Gives the user visibility to tickets specifically related to
 * the smarttrak project and allows them to add tickets to that project.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Eric Damschroder
 * @version 1.0
 * @since Dec 6, 2017
 ****************************************************************************/

public class ZohoIntegrationAction extends TicketFacadeAction {

	// ID for the Smarttrak Development project
	public static final String PROJECT_ID = "967168000000259067";
	public ZohoIntegrationAction() {
		super();
	}

	public ZohoIntegrationAction(ActionInitVO init) {
		super(init);
	}
	
	/**
	 * This action will only ever look at tickets for the Smarttrak Development project
	 * As such set those parameters here to ensure that they cannot be overridden
	 * to give the user access to areas they are not supposed to be able to reach.
	 */
	public void retrieve(ActionRequest req) throws ActionException {
		req.setParameter("projectId", PROJECT_ID);
		req.setParameter(TICKET_TYPE_PARAM, TICKET_TYPE.BUG.toString());
		if (Convert.formatBoolean(req.getParameter("loadData"))) {
			super.list(req);
		} else {
			// Only load the support fields if they are not present in
			// order to reduce calls out to zoho for information.
			loadSupportFields(req);
		}
	}
	
	/**
	 * Get the list of support fields for the add form
	 * @param req
	 * @throws ActionException
	 */
	private void loadSupportFields(ActionRequest req) throws ActionException {
		SupportFieldsVO fields = SupportUtilFactory.getSupportUtil(attributes, req).getSupportFields();
		req.setAttribute("supportFields", fields);
	}

	public void build(ActionRequest req) throws ActionException {
		req.setParameter("projectId", PROJECT_ID);
		req.setParameter(TICKET_TYPE_PARAM, TICKET_TYPE.BUG.toString());
		super.build(req);
	}

}
