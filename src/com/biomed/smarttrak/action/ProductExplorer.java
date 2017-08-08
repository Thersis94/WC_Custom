package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.common.SolrDocument;

import com.biomed.smarttrak.admin.SectionHierarchyAction;
import com.biomed.smarttrak.vo.ProductExplorerReportVO;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
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
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.util.MessageSender;

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

	private static final List<String> enumNames = buildEnumList();
	private static final String SAVED_QUERIES = "savedQueries";

	private enum BuildType {
		EXPORT, SAVE, SHARE, DELETE, HIERARCHY
	}

	/**
	 * Keep track of all solr compliant form fields and thier
	 * solr equivalents
	 */
	private enum SearchField {
		PRODUCT(true, SearchDocumentHandler.TITLE, "Product Name"),
		STATE(true, SearchDocumentHandler.STATE, "State"),
		COUNTRY(false, SearchDocumentHandler.COUNTRY, "Country"),
		COMPANY(true, "companysearch_s", "Company Name"),
		SEGMENT(false, SearchDocumentHandler.SECTION, "Segment"),
		MARKET(false, "target_market_ss", "Target Market"),
		INDICATION(false, "indication_ss", "Indication"),
		TECH(false, "technology_ss", "Technology"),
		APPROACH(false, "approach_ss", "Approach"),
		CLASSIFICATION(false, "classification_ss", "Classification"),
		INTREG(false, "intregionnm_ss", "International Region"),
		INTPATH(false, "intpathnm_ss", "International Path"),
		INTSTATUS(false, "intstatusnm_ss", "International Status"),
		USPATH(false, "uspathnm_ss", "US Path"),
		USSTATUS(false, "usstatusnm_ss", "US Status"),
		ALLY(true, "allysearch_ss", "Ally"),
		OWNERSHIP(false, "ownership_s", "Owner"),
		ID(false, SearchDocumentHandler.DOCUMENT_ID, "Product Id");

		private boolean contains;
		private String solrField;
		private String fieldName;

		SearchField(boolean contains, String solrField, String fieldName) {
			this.contains = contains;
			this.solrField = solrField;
			this.fieldName = fieldName;
		}

		public String getSolrField() {
			return solrField;
		}

		public String getFieldname() {
			return fieldName;
		}

		public boolean isContains() {
			return contains;
		}
	}

	public ProductExplorer() {
		super();
	}

	public ProductExplorer(ActionInitVO init) {
		super(init);
	}


	/**
	 * Build the list of parameters that can be made into enums so that
	 * they can be caught before throwing exceptions
	 * this can be done statically, since a List based on an enum can never change at runtime.
	 */
	protected static List<String> buildEnumList() {
		SearchField[] enums = SearchField.values();
		List<String> enumNames = new ArrayList<>(enums.length);
		for (SearchField s : enums) {
			enumNames.add(s.toString());
		}
		return enumNames;
	}


	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}


	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		putModuleData(retrieveProducts(req, false));
		if (req.getSession().getAttribute(SAVED_QUERIES) == null)
			retrieveSavedQueries(req);
	}


	/**
	 * Build a text representation of the filters applied to the search
	 */
	private void buildFilterList(ActionRequest req) {
		StringBuilder text = new StringBuilder(512);
		buildQueryFilters(text, req);
		if (req.hasParameter("selNodes")) {
			text.append(buildHierarchyFilters(req));
		} else {
			text.append("All Markets.");
		}
		req.getSession().setAttribute("filterList", text.toString());
	}


	/**
	 * Build the textual represtation of the hierarchies selected 
	 * for the supplied search.
	 */
	private String buildHierarchyFilters(ActionRequest req) {
		SectionHierarchyAction c = new SectionHierarchyAction();
		c.setActionInit(actionInit);
		c.setAttributes(attributes);
		c.setDBConnection(dbConn);
		Tree t = c.loadDefaultTree();

		StringBuilder part = new StringBuilder(128);
		String nodes = req.getParameter("selNodes");
		for (String s : nodes.split(",")) {
			Node n = t.findNode(s);
			if (n == null || n.getDepthLevel() == 2) continue;
			if (part.length() < 2) {
				part.append("in ");
			} else {
				part.append(", ");
			}
			part.append(n.getNodeName());
		}
		part.append(".");
		return part.toString();
	}


	/**
	 * Build the textual representation of the form elements of the supplied
	 * search
	 */
	private void buildQueryFilters(StringBuilder text, ActionRequest req) {
		for (String name : req.getParameterMap().keySet()) {
			// If this value is not in the list of enums skip it.
			if (!enumNames.contains(name) || StringUtil.isEmpty(req.getParameter(name))) continue;
			StringBuilder part = new StringBuilder(128);

			for (String value : req.getParameterValues(name)) {
				if (part.length() > 0) {
					part.append(" or ");
				} else {
					part.append(SearchField.valueOf(name).getFieldname()).append(" are ");
				}
				part.append(value);
			}

			text.append(part).append(". ");
		}
	}


	/**
	 * Get all saved queries for this user
	 * @param req
	 * @throws ActionException
	 */
	private void retrieveSavedQueries(ActionRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(125);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_EXPLORER_QUERY WHERE USER_ID = ? ");

		List<Map<String, String>> queries = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			UserVO user = (UserVO) req.getSession().getAttribute(Constants.USER_DATA);
			ps.setString(1, user.getUserId());

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				Map<String, String> entry = new HashMap<>();
				entry.put("id", rs.getString("EXPLORER_QUERY_ID"));
				entry.put("name", rs.getString("QUERY_NM"));
				entry.put("url", rs.getString("QUERY_TXT"));
				queries.add(entry);
			}

		} catch (SQLException e) {
			throw new ActionException(e);
		}

		req.getSession().setAttribute(SAVED_QUERIES, queries);
	}


	/**
	 * Get all products according to supplied filters
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected SolrResponseVO retrieveProducts(ActionRequest req, boolean getAll) throws ActionException {
		SolrActionVO qData = buildSolrAction(req, 100*Convert.formatInteger(req.getParameter("page")));

		buildSearchParams(req, qData);
		if (req.hasParameter("selNodes")) buildNodeParams(req, qData);

		addFacetFields(req, qData);

		SolrQueryProcessor sqp = new SolrQueryProcessor(attributes, qData.getSolrCollectionPath());
		
		SolrResponseVO vo = sqp.processQuery(qData);
		
		// Check to see if all the results should be returned instead of the current page
		if (getAll) {
			getRemainingDocuments(vo, sqp, qData);
		}

		if (!req.hasParameter("compare") && !req.hasParameter("textCompare"))
			buildFilterList(req);
		return vo;
	}


	/**
	 * Get all results for the supplied search
	 * @param vo
	 * @param sqp
	 * @param qData
	 */
	private void getRemainingDocuments(SolrResponseVO vo, SolrQueryProcessor sqp, SolrActionVO qData) {
		int totals = 0;
		List<SolrDocument> docs = new ArrayList<>();
		docs.addAll(vo.getResultDocuments());
		
		while (totals < vo.getTotalResponses()) {
			totals += 100;
			qData.setStartLocation(totals);
			SolrResponseVO currentResponse = sqp.processQuery(qData);
			docs.addAll(currentResponse.getResultDocuments());
		}
		
		vo.setResultDocuments(docs, 0, docs.size());
	}

	/**
	 * Add the facet filters to the qData. 
	 * @param compare
	 * @param qData
	 * @return
	 */
	protected void addFacetFields(ActionRequest req, SolrActionVO qData) {
		// Product Compare only uses two facets.
		if (!Convert.formatBoolean(req.getParameter("compare"))) {
			qData.addSolrField(new SolrFieldVO(FieldType.FACET, "target_market_ss", null, null));
			qData.addSolrField(new SolrFieldVO(FieldType.FACET, "indication_ss", null, null));
			qData.addSolrField(new SolrFieldVO(FieldType.FACET, "approach_ss", null, null));

			//Text compare only uses five
			if (!Convert.formatBoolean(req.getParameter("textCompare"))) {
				qData.addSolrField(new SolrFieldVO(FieldType.FACET, SearchDocumentHandler.SECTION, null, null));
				qData.addSolrField(new SolrFieldVO(FieldType.FACET, "intregionnm_ss", null, null));
				qData.addSolrField(new SolrFieldVO(FieldType.FACET, "intstatusnm_ss", null, null));
				qData.addSolrField(new SolrFieldVO(FieldType.FACET, "intpathnm_ss", null, null));
				qData.addSolrField(new SolrFieldVO(FieldType.FACET, "usstatusnm_ss", null, null));
				qData.addSolrField(new SolrFieldVO(FieldType.FACET, "uspathnm_ss", null, null));
				qData.addSolrField(new SolrFieldVO(FieldType.FACET, "company_s", null, null));
				qData.addSolrField(new SolrFieldVO(FieldType.FACET, "ownership_s", null, null));
				qData.addSolrField(new SolrFieldVO(FieldType.FACET, SearchDocumentHandler.HIERARCHY, null, null));
				qData.addSolrField(new SolrFieldVO(FieldType.FACET, SearchDocumentHandler.COUNTRY, null, null));
			}
		}
		qData.addSolrField(new SolrFieldVO(FieldType.FACET, "classification_ss", null, null));
		qData.addSolrField(new SolrFieldVO(FieldType.FACET, "technology_ss", null, null));
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
		String nodes = req.getParameter("selNodes");
		for (String s : nodes.split(",")) {
			if (selected.length() > 2) selected.append(" OR ");
			selected.append(s.replace("~", "\\~").replace(" ", "\\ ")).append("*");
		}
		selected.append(")");
		qData.addSolrField(new SolrFieldVO(FieldType.FILTER, "sectionid_ss", selected.toString(), BooleanType.AND));
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
				value.append("*").append(StringEscapeUtils.unescapeHtml(s).replace(" ", "\\ ").toLowerCase()).append("*");
			}
			return value.toString();
		}

		value.append("(");
		for (String s : parameterValues) {
			if (value.length() > 2) value.append(" or ");
			value.append("*").append(StringEscapeUtils.unescapeHtml(s).replace(" ", "\\ ").toLowerCase()).append("*");
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

		if (parameterValues.length == 1) return "\"" + StringEscapeUtils.unescapeHtml(parameterValues[0]).replace("(", "\\(").replace(")", "\\)") + "\"";

		StringBuilder value = new StringBuilder(50);
		value.append("(");
		for (String s : parameterValues) {
			if (value.length() > 2) value.append(" or ");
			value.append("\"").append(StringEscapeUtils.unescapeHtml(s).replace("(", "\\(").replace("(", "\\)")).append("\"");
		}
		value.append(")");

		return value.toString();
	}


	/**
	 * Get the solr information and set the standard parameters
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected SolrActionVO buildSolrAction(ActionRequest req, int start) throws ActionException {
		SolrAction sa = new SolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		
		// Retrieve the VO and set the base parameters
		SolrActionVO qData = sa.retrieveActionData(req);
		SBUserRole roles = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		qData.setRoleLevel(roles.getRoleLevel());
		qData.setRoleACL(roles.getAccessControlList());
		qData.setAclTypeNo(10);
		qData.setStartLocation(start);
		
		return qData;
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		BuildType type = BuildType.valueOf(req.getParameter("buildType"));

		switch (type) {
			case EXPORT:
				exportResults(req);
				break;
			case SAVE:
				saveQuery(req);
				break;
			case DELETE:
				deleteQuery(req);
				break;
			case SHARE:
				sendEmail(req);
				break;
			case HIERARCHY:
				getHierarchy(req);
				break;
		}
	}


	/**
	 * Get the complete hierarchy list
	 * @param req
	 * @throws ActionException
	 */
	private void getHierarchy(ActionRequest req) throws ActionException {
		Set<String> allowedSections = getPopulatedHierarchies(req);
		
		SectionHierarchyAction sha = new SectionHierarchyAction();
		sha.setAttributes(getAttributes());
		sha.setDBConnection(getDBConnection());
		Tree t = sha.loadTree(req.getParameter("sectionId"));
		
		putModuleData(filterTree(allowedSections, t.preorderList(true)));
	}
	
	/**
	 * Filter the list of sections by the list of sections that have content
	 * and can be viewed by the logged in user.
	 * @param allowedSections
	 * @param fullList
	 * @return
	 */
	private List<Node> filterTree(Set<String> allowedSections, List<Node> fullList) {
		List<Node> sections = new ArrayList<>();
		
		for (Node n : fullList) {
			FilterNode(allowedSections, n, sections);
		}
		
		return sections;
	}
	
	/**
	 * Determine whether the current node has associated products. 
	 * If so do the same for the node's children
	 * @param allowedSections
	 * @param n
	 * @param sections
	 */
	private void FilterNode(Set<String> allowedSections, Node n, List<Node> sections) {
		if (n == null || !allowedSections.contains(n.getNodeName())) return;
		
		if (n.getNumberChildren() > 0) {
			List<Node> children = new ArrayList<>();
			for (Node child : n.getChildren()) {
				if (allowedSections.contains(child.getNodeName())) {
					children.add(child);
				}
			}
			n.setChildren(children);
		}
		
		sections.add(n);
	}

	
	/**
	 * Query solr to get the hierarchies that have products associated with them.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private Set<String> getPopulatedHierarchies(ActionRequest req) throws ActionException {
		SolrActionVO qData = buildSolrAction(req, 0);
		
		qData.addSolrField(new SolrFieldVO(FieldType.FACET, SearchDocumentHandler.HIERARCHY, null, null));

		SolrQueryProcessor sqp = new SolrQueryProcessor(attributes, qData.getSolrCollectionPath());
		SolrResponseVO vo = sqp.processQuery(qData);
		
		Set<String> allowedSections = new HashSet<>();
		for (Count c : vo.getFacetByName(SearchDocumentHandler.HIERARCHY)) {
			if (c.getCount() == 0) continue;
			
			for (String section : c.getName().split(SearchDocumentHandler.HIERARCHY_DELIMITER)) {
				allowedSections.add(section);
			}
		}
		return allowedSections;
	}

	/**
	 * Delete the supplied saved query
	 * @param req
	 * @throws ActionException 
	 */
	@SuppressWarnings("unchecked")
	private void deleteQuery(ActionRequest req) throws ActionException {
		String id = req.getParameter("queryId");
		
		StringBuilder sql = new StringBuilder(150);
		sql.append("DELETE FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_EXPLORER_QUERY WHERE EXPLORER_QUERY_ID = ? ");
		log.debug(sql+"|"+id);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, id);
			
			ps.executeUpdate();
			
		} catch(SQLException e) {
			log.error(e);
			throw new ActionException(e);
		}
		
		putModuleData(id);
		List<Map<String, String>> saved = (List<Map<String, String>>)req.getSession().getAttribute(SAVED_QUERIES);
		if (saved != null && id != null)
			updateSessionQueries(id, saved, req);
	}
	

	private void updateSessionQueries(String id, List<Map<String, String>> saved, ActionRequest req) {
		List<Map<String, String>> newList = new ArrayList<>();
		for (Map<String, String> entry : saved) {
			if(!id.equals(entry.get("id")))
				newList.add(entry);
		}
		
		req.getSession().setAttribute(SAVED_QUERIES, newList);
	}

	/**
	 * Create a report vo that can be used to return an excel document
	 * containing all currently displayed products.
	 * @param req
	 * @throws ActionException
	 */
	protected void exportResults(ActionRequest req) throws ActionException {
		AbstractSBReportVO report = new ProductExplorerReportVO();
		report.setData(retrieveProducts(req, true).getResultDocuments());
		report.setFileName("Product Set " + Convert.getCurrentTimestamp() + ".xls");
		req.setAttribute(Constants.BINARY_DOCUMENT, report);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
	}


	/**
	 * Send an email with the supplied query to the supplied individual
	 * @param req
	 * @throws ActionException
	 */
	protected void sendEmail(ActionRequest req) throws ActionException {
		try {
			UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
			EmailMessageVO msg = new EmailMessageVO();
			msg.setSubject("Smarttrak Products");
			msg.addRecipient(req.getParameter("recipient"));

			StringBuilder body = new StringBuilder(250);
			body.append(user.getFullName()).append(" has shared a product set with you.</br>");
			body.append("You can view the product set <a href='").append(buildUrl(req));
			body.append("'>here</a>.");

			msg.setHtmlBody(body.toString());
			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(msg);
		} catch (InvalidDataException e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Save the supplied query to the database.
	 * @param req
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	protected void saveQuery(ActionRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(200);
		sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_EXPLORER_QUERY (EXPLORER_QUERY_ID, USER_ID, QUERY_NM, QUERY_TXT, CREATE_DT) ");
		sql.append("VALUES(?,?,?,?,?)");
		Map<String, String> entry = new HashMap<>();
		entry.put("id", new UUIDGenerator().getUUID());
		entry.put("name", req.getParameter("queryName"));
		entry.put("url", buildUrl(req));
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			UserVO user = (UserVO) req.getSession().getAttribute(Constants.USER_DATA);
			ps.setString(1, entry.get("id"));
			ps.setString(2, user.getUserId());
			ps.setString(3, entry.get("name"));
			ps.setString(4, entry.get("url"));
			ps.setTimestamp(5, Convert.getCurrentTimestamp());

			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		
		super.putModuleData(entry);

		if (req.getSession().getAttribute(SAVED_QUERIES) == null)
			req.getSession().setAttribute(SAVED_QUERIES, new ArrayList<Map<String, String>>());

		((List<Map<String, String>>)req.getSession().getAttribute(SAVED_QUERIES)).add(entry);
	}


	/**
	 * Build a url that can be used to reload a saved product set from solr.
	 * @param req
	 * @return
	 */
	protected String buildUrl(ActionRequest req) {
		StringBuilder url = new StringBuilder(100);
		if (req.hasParameter("url")) {
			SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
			url.append(site.getFullSiteAlias());
			url.append(req.getParameter("url"));
		}
		url.append("?load=true");
		for (String name : req.getParameterMap().keySet()) {
			// If this value is not in the list of enums skip it.
			if (!enumNames.contains(name) || StringUtil.isEmpty(req.getParameter(name))) continue;

			for (String value : req.getParameterValues(name)) {
				url.append("&").append(name).append("=").append(value);
			}
		}
		url.append("&selNodes=").append(req.getParameter("selNodes"));

		return url.toString();
	}
}
