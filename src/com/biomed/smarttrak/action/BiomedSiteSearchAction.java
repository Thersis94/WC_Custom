package com.biomed.smarttrak.action;

import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.biomed.smarttrak.util.BiomedInsightIndexer;
import com.biomed.smarttrak.util.UpdateIndexer;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;

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
		//fail fast is there's no search going on
		if (!req.hasParameter("searchData")) return;
		List<SolrResponseVO> resp = new ArrayList<>();
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);

		String searchData = StringUtil.checkVal(req.getParameter("searchData"));
		req.setParameter("searchData", searchData.toLowerCase(), true);

		req.setAttribute(SmarttrakSolrAction.SECTION, SmarttrakSolrAction.BROWSE_SECTION);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		req.setParameter("pmid", mod.getPageModuleId());
		resp.add(getResults(req));

		// If fq parameters are defined use them of setting them on the update and insight searches
		boolean hasFq = req.hasParameter("fq");

		// Counts are always required for everything so these searches still need to be done
		if (!hasFq)
			req.setParameter("fq", SearchDocumentHandler.INDEX_TYPE + ":" + BiomedInsightIndexer.INDEX_TYPE);
		req.setAttribute(SmarttrakSolrAction.SECTION, Section.INSIGHT);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_2));
		resp.add(getResults(req));

		//query updates separtely from insights, because they use a different ACL
		// If fq parameters are defined use them
		if (!hasFq)
			req.setParameter("fq", SearchDocumentHandler.INDEX_TYPE + ":" + UpdateIndexer.INDEX_TYPE);
		req.setAttribute(SmarttrakSolrAction.SECTION, Section.UPDATES_EDITION);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_2));
		resp.add(getResults(req));
		/*
		 * unclear why this is needed, but it is.  If we don't flush the FQ in gets picked-up by the 1st query.  yes, illogical!  -JM- 08.29.17
		 * 
		 * Update - This is necessary in the use case where you're on the expanded search results page.  The action gets processed twice,
		 * Once for the Search box in the upper right, and a second time for the main column results.  with fq being set, the results of the first
		 * iteration are overriding hasfq check for the second.  This ensures they are clear.  -BL- 04.11.19
		 */
		req.setParameter("fq", null);

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
		SolrAction sa = new SmarttrakSolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
		sa.retrieve(req);

		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		return (SolrResponseVO) mod.getActionData();
	}	
}