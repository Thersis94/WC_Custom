package com.biomed.smarttrak.admin;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;


/****************************************************************************
 * <b>Title</b>: SmartSearchAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Passes along information for a solr search specified by 
 * the admin controller.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * @author Eric Damschroder
 * @version 1.0
 * @since Jan 25, 2018
 ****************************************************************************/
public class SmartSearchAction extends SBActionAdapter {

	public SmartSearchAction() {
		super();
	}

	public SmartSearchAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// Pass along the proper information for a search to be done.
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_2));
		req.setParameter("pmid", mod.getPageModuleId());
		String search = StringUtil.checkVal(req.getParameter("searchData"));

		req.setParameter("searchData", search.toLowerCase());

		// Build the solr action
		SolrAction sa = new SolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
		sa.retrieve(req);

		req.setParameter("searchData", search);
	}
}