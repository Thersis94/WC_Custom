
package com.biomed.smarttrak.action;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery.ORDER;

import com.biomed.smarttrak.solr.BiomedInsightIndexer;
import com.biomed.smarttrak.vo.InsightVO;
import com.biomed.smarttrak.util.UpdateIndexer;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrActionIndexVO;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrFieldVO;
import com.smt.sitebuilder.action.search.SolrFieldVO.BooleanType;
import com.smt.sitebuilder.action.search.SolrFieldVO.FieldType;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
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

		if(req.hasParameter(INSIGHT_ID)){
			getInsightById(StringUtil.checkVal(req.getParameter(INSIGHT_ID)));
		}else{
			putModuleData(retrieveInsights(req));
		}
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

	/**
	 * Get the solr information 
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private SolrActionVO buildSolrAction(ActionRequest req) throws ActionException {


		//Set SolrSearch ActionId on actionInit
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		//TODO
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));

		//Get SolrSearch ActionVO.
		SolrAction sa = new SolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
		return sa.retrieveActionData(req);
	}

	/**
	 * Retrieve all products from solr according to the search terms
	 * @param req
	 * @throws ActionException
	 */
	protected SolrResponseVO retrieveInsights(ActionRequest req) throws ActionException {
		log.debug("######### retrieve insights called");
		SolrActionVO qData = buildSolrAction(req);
		SolrQueryProcessor sqp = new SolrQueryProcessor(attributes, qData.getSolrCollectionPath());
		qData.setNumberResponses(15);
		qData.setStartLocation(Convert.formatInteger(req.getParameter("pageNo"), 0) * 15);
		qData.setOrganizationId(((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId());
		qData.setRoleLevel(10);
		qData.addSolrField(new SolrFieldVO(FieldType.BOOST, SearchDocumentHandler.TITLE, "", BooleanType.AND));
		qData.addSolrField(new SolrFieldVO(FieldType.BOOST, SearchDocumentHandler.HIERARCHY, "", BooleanType.AND));
		qData.addSolrField(new SolrFieldVO(FieldType.FACET, SearchDocumentHandler.HIERARCHY, null, null));

		//Add Search Parameters.
		if (req.hasParameter("searchData")) 
			qData.setSearchData("*"+req.getParameter("searchData")+"*");

		//Add Sections Check.
		if (req.hasParameter("hierarchyId")) {
			StringBuilder selected = new StringBuilder(50);
			selected.append("(");
			for (String s : req.getParameterValues("hierarchyId")) {
				if (selected.length() > 2) selected.append(" OR ");
				selected.append("*").append(s);
			}
			selected.append(")");
			qData.addSolrField(new SolrFieldVO(FieldType.FILTER, SearchDocumentHandler.HIERARCHY, selected.toString(), BooleanType.AND));
		}

		//Get a Date Range String.
		String dates = SolrActionUtil.makeRangeQuery(FieldType.DATE, req.getParameter("startDt"), req.getParameter("endDt"));

		//If we have a date range, add it as a solr field.
		if(!StringUtil.isEmpty(dates)) {
			qData.addSolrField(new SolrFieldVO(FieldType.FILTER, SearchDocumentHandler.UPDATE_DATE, dates, BooleanType.AND));
		}

		//Add TypeId
		if(req.hasParameter("typeId")) {
			qData.addSolrField(new SolrFieldVO(FieldType.FILTER, SearchDocumentHandler.MODULE_TYPE, req.getParameter("typeId"), BooleanType.AND));
		}

		qData.addIndexType(new SolrActionIndexVO("", BiomedInsightIndexer.INDEX_TYPE));

		qData.setFieldSort(SearchDocumentHandler.UPDATE_DATE);
		qData.setSortDirection(ORDER.desc);

		return sqp.processQuery(qData);
	}
}