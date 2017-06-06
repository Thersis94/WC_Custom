package com.biomed.smarttrak.admin;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

public class DashboardAction extends SBActionAdapter {

	public DashboardAction() {
		super();
	}

	public DashboardAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Searching");
		// Pass along the proper information for a search to be done.
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		log.debug(actionInit);
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
