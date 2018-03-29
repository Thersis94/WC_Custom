package com.depuysynthes.srt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.depuysynthes.srt.util.SRTUtil;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> SRTSearchAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> SRT Search Tool
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Mar 27, 2018
 ****************************************************************************/
public class SRTSearchAction extends SimpleActionAdapter {

	public SRTSearchAction() {
		super();
	}

	public SRTSearchAction(ActionInitVO init) {
		super(init);
	}

	/**
	 * Get the results of the two solr search associate to this widget and put them into a list
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.hasParameter("export")) {
			exportResults(req);
		} else if(req.hasParameter("searchData")){
			searchSolr(req);
		}
	}


	/**
	 * Helper method that runs search against solr for all DocumentIds that
	 * match loads Data for Projects, then exports data to Excel Sheet.
	 * @param req
	 * @throws ActionException
	 */
	private void exportResults(ActionRequest req) throws ActionException {
		SolrResponseVO resp;
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);

		prepSolrRequest(req);

		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		req.setParameter("pmid", mod.getPageModuleId());
		resp = getResults(req);

		List<String> projectIds = getAllData(resp, req);
	}

	/**
	 * Gather all the projectIds from the solr response and return list of
	 * projectIds.
	 * @param resp
	 * @return
	 */
	private List<String> getAllData(SolrResponseVO resp, ActionRequest req) {

		return null;
	}

	/**
	 * Run standard Search against Solr.
	 * @param req
	 * @throws ActionException
	 */
	private void searchSolr(ActionRequest req) throws ActionException {
		String searchData = StringUtil.checkVal(req.getParameter("searchData"));
		SolrResponseVO resp;
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);

		prepSolrRequest(req);

		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		req.setParameter("pmid", mod.getPageModuleId());
		resp = getResults(req);

		putModuleData(resp, (int)resp.getTotalResponses(), false);
		req.setParameter("searchData", searchData, true);
	}

	/**
	 * Manages Transposing and setting any relevant params on the req before
	 * solr search is called.
	 * @param req
	 */
	public void prepSolrRequest(ActionRequest req) {

		//Set Full Match Query
		req.setParameter("searchData", "*:*", true);

		//Convert Bootstrap Table Pagination to Solr Pagination
		if(req.hasParameter("limit")) {
			req.setParameter("rpp", req.getParameter("limit"));
			req.setParameter("page", Integer.toString(req.getIntegerParameter("offset") / req.getIntegerParameter("limit", 25)));
		}

		//Add OpCo FQ to list of fqs on the request.
		List<String> values = new ArrayList<>();
		if(req.hasParameter("fq"))
			values.addAll(Arrays.asList(req.getParameterValues("fq")));

		values.add("opCoId_s:" + SRTUtil.getOpCO(req));
		req.setParameter("fq", values.toArray(new String [values.size()]), true);
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