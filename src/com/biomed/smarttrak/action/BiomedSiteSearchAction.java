package com.biomed.smarttrak.action;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: BiomedSiteSearchAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Handles the two solr searches with separate sort fields
 *  that are needed in order to properly put together the site search results.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Eric Damschroder
 * @version 1.0
 * @since Apr 11, 2017
 ****************************************************************************/

public class BiomedSiteSearchAction extends SBActionAdapter {
	
	public BiomedSiteSearchAction() {
		super();
	}

	public BiomedSiteSearchAction(ActionInitVO init) {
		super(init);
	}
	
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}

	
	/**
	 * Get the results of the two solr search associate to this widget and put them into a list
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
    	List<SolrResponseVO> resp = new ArrayList<>();
    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
    	
    	String searchData = StringUtil.checkVal(req.getParameter("searchData"));
    	req.setParameter("searchData", searchData.toLowerCase(), true);
    	
    	actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
    	req.setParameter("pmid", mod.getPageModuleId());
    	resp.add(getResults(req));
	
    	actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_2));
    	resp.add(getResults(req));
		
		putModuleData(resp);
    	req.setParameter("searchData", searchData, true);
	}

	
	/**
	 * Create a solr action and call retrieve on it to get the search results
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private SolrResponseVO getResults(ActionRequest req) throws ActionException {
	    	// Build the solr action
		SolrAction sa = new SolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
		sa.retrieve(req);

		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		return (SolrResponseVO) mod.getActionData();
	}
	
}
