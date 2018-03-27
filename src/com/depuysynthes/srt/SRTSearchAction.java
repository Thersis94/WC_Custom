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

		List<SolrResponseVO> resp = new ArrayList<>();
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);

		String searchData = StringUtil.checkVal(req.getParameter("searchData"));
		req.setParameter("searchData", searchData.toLowerCase(), true);

		List<String> values = new ArrayList<>();
		if(req.hasParameter("fq")) 
			values.addAll(Arrays.asList(req.getParameterValues("fq")));

		values.add("opCoId_s:" + SRTUtil.getOpCO(req));
		req.setParameter("fq", values.toArray(new String [values.size()]), true);

		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		req.setParameter("pmid", mod.getPageModuleId());
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