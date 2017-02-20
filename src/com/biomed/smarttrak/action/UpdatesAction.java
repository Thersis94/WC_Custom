/**
 *
 */
package com.biomed.smarttrak.action;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.solr.client.solrj.SolrQuery.ORDER;

import com.biomed.smarttrak.solr.BiomedUpdateIndexer;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
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

/****************************************************************************
 * <b>Title</b>: UpdatesAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Public Updates Action that talks to Solr.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 16, 2017
 ****************************************************************************/
public class UpdatesAction extends SBActionAdapter {

	public void retrieve(ActionRequest req) throws ActionException {
		putModuleData(retrieveUpdates(req));
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
	protected SolrResponseVO retrieveUpdates(ActionRequest req) throws ActionException {
		SolrActionVO qData = buildSolrAction(req);
		SolrQueryProcessor sqp = new SolrQueryProcessor(attributes, qData.getSolrCollectionPath());
		qData.setNumberResponses(15);
		qData.setStartLocation(Convert.formatInteger(req.getParameter("pageNo"), 0) * 15);
		qData.setOrganizationId(((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId());
		qData.setRoleLevel(AdminControllerAction.DEFAULT_ROLE_LEVEL);
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

		//Add Start - End Date Range
		StringBuilder dates = new StringBuilder(75);

		if (req.hasParameter("startDt") && req.hasParameter("endDt")) {

			//Parse Start Date
			ZonedDateTime sdz = getSolrDate(req.getParameter("startDt"));

			//Parse End Date
			ZonedDateTime edz = getSolrDate(req.getParameter("endDt"));

			dates.append("[").append(DateTimeFormatter.ISO_INSTANT.format(sdz));
			dates.append(" TO ").append(DateTimeFormatter.ISO_INSTANT.format(edz));
			dates.append("]");

		} else if(req.hasParameter("startDt")) {
			ZonedDateTime sdz = getSolrDate(req.getParameter("startDt"));
	    	dates.append("[").append(DateTimeFormatter.ISO_INSTANT.format(sdz)).append(" TO *]");
		} else if(req.hasParameter("endDt")) {
			ZonedDateTime edz = getSolrDate(req.getParameter("endDt"));
	    	dates.append("[* TO ").append(DateTimeFormatter.ISO_INSTANT.format(edz)).append("]");
		}

		//If we have a date range, add it as a solr field.
		if(dates.length() > 0) {
			qData.addSolrField(new SolrFieldVO(FieldType.FILTER, SearchDocumentHandler.UPDATE_DATE, dates.toString(), BooleanType.AND));
		}

		//Add TypeId
		if(req.hasParameter("typeId")) {
			qData.addSolrField(new SolrFieldVO(FieldType.FILTER, SearchDocumentHandler.MODULE_TYPE, req.getParameter("typeId"), BooleanType.AND));
		}

		qData.addIndexType(new SolrActionIndexVO("", BiomedUpdateIndexer.INDEX_TYPE));

		qData.setFieldSort(SearchDocumentHandler.UPDATE_DATE);
		qData.setSortDirection(ORDER.desc);

		return sqp.processQuery(qData);
	}

	protected ZonedDateTime getSolrDate(String dateText) {
		ZonedDateTime zdt = null;
		try {
			LocalDateTime ldt = LocalDateTime.from(LocalDate.parse(dateText, DateTimeFormatter.ofPattern("MM/dd/yyyy")).atStartOfDay());
			zdt = ZonedDateTime.of(ldt, ZoneId.systemDefault());
		} catch (Exception e) {
			log.error("Could not parse date : " + dateText, e);
		}
		return zdt;
	}
}