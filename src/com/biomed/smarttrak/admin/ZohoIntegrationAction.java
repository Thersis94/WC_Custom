package com.biomed.smarttrak.admin;


import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.ticketing.SupportUtilFactory;
import com.smt.sitebuilder.action.ticketing.TicketFacadeAction;
import com.smt.sitebuilder.action.ticketing.vo.SupportFieldsVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.CacheAdministrator;

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
	private static final String SMARTTRAK_CACHE_KEY = "smarttrak_zoho_ticket_list";
	private static final int CACHE_TIMEOUT = 900;
	
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
			retrieveTickets(req);
		} else {
			// Only load the support fields if they are not present in
			// order to reduce calls out to zoho for information.
			loadSupportFields(req);
		}
	}
	
	/**
	 * Determine whether the ticket information can be laoded from cache or if it needs to
	 * be loaded from zoho. If it is loaded from zoho ensure that it is cached as well.
	 * @param req
	 * @throws ActionException
	 */
	private void retrieveTickets(ActionRequest req) throws ActionException {
		CacheAdministrator cache = new CacheAdministrator(attributes);
		ModuleVO cachedMod = cache.readFromCache(SMARTTRAK_CACHE_KEY);
		if (cachedMod == null) {
			super.list(req);
			cachedMod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
			cachedMod.setCacheTimeout(CACHE_TIMEOUT);
			cachedMod.setPageModuleId(SMARTTRAK_CACHE_KEY);
			cache.writeToCache(cachedMod);
		} else {
			super.putModuleData(cachedMod.getActionData(), cachedMod.getDataSize(), false);
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

		CacheAdministrator cache = new CacheAdministrator(attributes);
		cache.clearCacheByKey(SMARTTRAK_CACHE_KEY);
	}

}
