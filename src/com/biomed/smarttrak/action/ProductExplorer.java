package com.biomed.smarttrak.action;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.solr.client.solrj.SolrQuery.ORDER;

import com.biomed.smarttrak.util.BiomedProductIndexer;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrActionIndexVO;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrFieldVO;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.action.search.SolrFieldVO.BooleanType;
import com.smt.sitebuilder.action.search.SolrFieldVO.FieldType;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>: ProductExplorer.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Handles complex filtering of all products in the solr.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Feb 17, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class ProductExplorer extends SBActionAdapter {
	
	private List<String> enumNames;
	
	/**
	 * Keep track of all solr compliant form fields and thier
	 * solr equivalents
	 */
	private enum SearchField {
		PRODUCT(true, SearchDocumentHandler.TITLE),
		COMPANY(true, "company_s"),
		SEGMENT(false, "sectionname_ss"),
		MARKET(false, "target_market_ss"),
		INDICATION(false, "indication_ss"),
		TECH(false, "technology_ss"),
		APPROACH(false, "approach_ss"),
		CLASSIFICATION(false, "classification_ss"),
		COUNTRY(false, SearchDocumentHandler.COUNTRY),
		INTREG(false, "intregionnm_ss"),
		INTPATH(false, "intpathnm_ss"),
		INTSTAT(false, "intstatusnm_ss"),
		USPATH(false, "uspathnm_ss"),
		USSTAT(false, "usstatus_ss"),
		STATE(false, SearchDocumentHandler.STATE),
		ALLY(true, "ally_ss"),
		ID(false, SearchDocumentHandler.DOCUMENT_ID);
		private boolean contains;
		private String solrField;
		
		SearchField(boolean contains, String solrField) {
			this.contains = contains;
			this.solrField = solrField;
		}
		
		public String getSolrField() {
			return solrField;
		}
		
		public boolean isContains() {
			return contains;
		}
	}
	
	public ProductExplorer() {
		super();
		buildEnumList();
	}

	public ProductExplorer(ActionInitVO init) {
		super(init);
		buildEnumList();
	}
	
	
	/**
	 * Build the list of parameters that can be made into enums so that
	 * they can be caught before throwing exceptions
	 */
	protected void buildEnumList() {
		SearchField[] enums = SearchField.values();
		enumNames = new ArrayList<>(enums.length);
		for (SearchField s : enums) {
			enumNames.add(s.toString());
		}
	}
	
	
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SolrActionVO qData = buildSolrAction(req);
		SolrQueryProcessor sqp = new SolrQueryProcessor(attributes, qData.getSolrCollectionPath());
		qData.setNumberResponses(100);
		qData.setStartLocation(Convert.formatInteger(req.getParameter("page"))*100);
		qData.setOrganizationId(((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId());
		qData.setRoleLevel(0);
		
		buildSearchParams(req, qData);
		if (req.hasParameter("selNodes")) buildNodeParams(req, qData);

		qData.addIndexType(new SolrActionIndexVO("", BiomedProductIndexer.INDEX_TYPE));

		qData.addSolrField(new SolrFieldVO(FieldType.FACET, "sectionname_ss", null, null));
		qData.addSolrField(new SolrFieldVO(FieldType.FACET, "target_market_ss", null, null));
		qData.addSolrField(new SolrFieldVO(FieldType.FACET, "indication_ss", null, null));
		qData.addSolrField(new SolrFieldVO(FieldType.FACET, "approach_ss", null, null));
		qData.addSolrField(new SolrFieldVO(FieldType.FACET, "technology_ss", null, null));
		qData.addSolrField(new SolrFieldVO(FieldType.FACET, "classification_ss", null, null));
		qData.addSolrField(new SolrFieldVO(FieldType.FACET, "country", null, null));
		qData.addSolrField(new SolrFieldVO(FieldType.FACET, "intregionnm_ss", null, null));
		qData.addSolrField(new SolrFieldVO(FieldType.FACET, "intstatusnm_ss", null, null));
		qData.addSolrField(new SolrFieldVO(FieldType.FACET, "intpathnm_ss", null, null));
		qData.addSolrField(new SolrFieldVO(FieldType.FACET, "usstatusnm_ss", null, null));
		qData.addSolrField(new SolrFieldVO(FieldType.FACET, "uspathnm_ss", null, null));
		
		qData.setFieldSort(SearchDocumentHandler.TITLE_LCASE);
		qData.setSortDirection(ORDER.asc);
		SolrResponseVO vo = sqp.processQuery(qData);
		super.putModuleData(vo);
	}
	
	
	/**
	 * Loop over the selected nodes in the hierarchy list and add them to 
	 * the solr request.
	 * @param req
	 * @param qData
	 */
	protected void buildNodeParams(ActionRequest req, SolrActionVO qData) {
		StringBuilder selected = new StringBuilder(50);
		selected.append("(");
		for (String s : req.getParameterValues("selNodes")) {
			if (selected.length() > 2) selected.append(" OR ");
			selected.append("*").append(s);
		}
		selected.append(")");
		qData.addSolrField(new SolrFieldVO(FieldType.FILTER, SearchDocumentHandler.SECTION, selected.toString(), BooleanType.AND));
	}

	
	/**
	 * Loop over parameters in the request object and, if they are listed as a 
	 * search param, create a filter field using that value and the matching
	 * solr field
	 * @param req
	 * @param qData
	 */
	protected void buildSearchParams(ActionRequest req, SolrActionVO qData) {
		for (String name : req.getParameterMap().keySet()) {
			// If this value is not in the list of enums skip it.
			if (!enumNames.contains(name) || StringUtil.isEmpty(req.getParameter(name))) continue;
			
			SearchField search = SearchField.valueOf(name);
			String value = buildValues(req.getParameterValues(name), search.isContains());
			qData.addSolrField(new SolrFieldVO(FieldType.FILTER, search.getSolrField(), value, BooleanType.AND));
		}
	}

	
	/**
	 * Build a solr compatible value field out of the supplied value array
	 * @param parameterValues
	 * @param contains
	 * @return
	 */
	protected String buildValues(String[] parameterValues, boolean contains) {
		
		if (contains) {
			return buildContainsValue(parameterValues);
		} else {
			return buildExactValue(parameterValues);
		}
	}


	/**
	 * Format the value for a proper contains search
	 * @param parameterValues
	 * @return
	 */
	protected String buildContainsValue(String[] parameterValues) {
		
		StringBuilder value = new StringBuilder(50);
		if (parameterValues.length == 1) {
			String singleValue = parameterValues[0];
			for (String s : singleValue.split(" ")) {
				if (value.length() > 2) value.append(" and ");
				value.append("*").append(StringEscapeUtils.unescapeHtml(s)).append("*");
			}
			return value.toString();
		}

		value.append("(");
		for (String s : parameterValues) {
			if (value.length() > 2) value.append(" or ");
			value.append("*").append(StringEscapeUtils.unescapeHtml(s).replace(" ", "\\ ")).append("*");
		}
		value.append(")");
		
		return value.toString();
	}

	
	/**
	 * Format the parameter properly for an exact match search
	 * @param parameterValues
	 * @return
	 */
	protected String buildExactValue(String[] parameterValues) {
		
		if (parameterValues.length == 1) return "\"" + parameterValues[0] + "\"";

		StringBuilder value = new StringBuilder(50);
		value.append("(");
		for (String s : parameterValues) {
			if (value.length() > 2) value.append(" or ");
			value.append("\"").append(StringEscapeUtils.unescapeHtml(s)).append("\"");
		}
		value.append(")");
		
		return value.toString();
	}

	
	/**
	 * Get the solr information 
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected SolrActionVO buildSolrAction(ActionRequest req) throws ActionException {
		SolrAction sa = new SolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
	    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
	    	actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		return sa.retrieveActionData(req);
	}

}
