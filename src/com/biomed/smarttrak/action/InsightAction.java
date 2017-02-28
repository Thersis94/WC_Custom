
package com.biomed.smarttrak.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import com.biomed.smarttrak.vo.InsightVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrFieldVO.FieldType;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

/****************************************************************************
 * <b>Title</b>: InsightAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Public Insights Action that talks to Solr.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Ryan Riker
 * @version 1.0
 * @since Feb 16, 2017
 ****************************************************************************/
public class InsightAction extends SBActionAdapter {
	private static final String INSIGHT_ID = "insightId";

	public void retrieve(ActionRequest req) throws ActionException {
		//check to see if we are getting an insite or a solr list
		if(req.hasParameter(INSIGHT_ID)){
			getInsightById(StringUtil.checkVal(req.getParameter(INSIGHT_ID)));
		}else{
			ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
			actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));

			//transform some incoming reqParams to where Solr expects to see them
			transposeRequest(req);

			//Get SolrSearch ActionVO.
			SolrAction sa = new SolrAction(actionInit);
			sa.setDBConnection(dbConn);
			sa.setAttributes(attributes);
			sa.retrieve(req);
		}
	}
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/**
	 * transpose incoming request parameters into values Solr understands, so they get executed for us.
	 * @param req
	 * @throws ActionException
	 */
	protected void transposeRequest(ActionRequest req) throws ActionException {
		//get the filter queries already on the request.  Add ours to the stack, and put the String[] back on the request for Solr
		String[] fqs = req.getParameterValues("fq");
		if (fqs == null) fqs = new String[0];
		List<String> data = new ArrayList<>(Arrays.asList(fqs));

		//Add Sections Check.  Append a filter query for each section requested
		if (req.hasParameter("hierarchyId")) {
			for (String s : req.getParameterValues("hierarchyId")){
				log.debug("########## hierarchyId" + s);
				data.add(SearchDocumentHandler.HIERARCHY + ":" + s);
			}
		}

		//Get a Date Range String.
		String dates = SolrActionUtil.makeRangeQuery(FieldType.DATE, req.getParameter("startDt"), req.getParameter("endDt"));
		if (!StringUtil.isEmpty(dates))
			data.add(SearchDocumentHandler.UPDATE_DATE + ":" + dates);

		//Add a ModuleType filter if typeId was passed
		if (req.hasParameter("typeId"))
			data.add(SearchDocumentHandler.MODULE_TYPE + ":" + req.getParameter("typeId"));

		//put the new list of filter queries back on the request
		req.setParameter("fq", data.toArray(new String[data.size()]), true);
	}
	
	
	/**
	 * @param checkVal
	 */
	private void getInsightById(String insightId) {
		log.debug("start get insight by id");
		
		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder sb = new StringBuilder(50);
		sb.append("select a.*, p.first_nm, p.last_nm, b.section_id ");
		sb.append("from ").append(schema).append("biomedgps_insight a ");
		sb.append("inner join profile p on a.creator_profile_id=p.profile_id ");
		sb.append("left outer join ").append(schema).append("biomedgps_insight_section b ");
		sb.append("on a.insight_id=b.insight_id ");
		sb.append("where a.insight_id = ? ");
		
		List<Object> params = new ArrayList<>();
		 params.add(insightId);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  insight = db.executeSelect(sb.toString(), params, new InsightVO());
		log.debug("loaded " + insight.size() + " insight");
		
		log.debug("placed vo on mod data: " + (InsightVO)insight.get(0));
		
		putModuleData((InsightVO)insight.get(0));
	}


}