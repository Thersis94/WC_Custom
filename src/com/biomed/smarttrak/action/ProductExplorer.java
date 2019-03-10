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
import com.biomed.smarttrak.security.SecurityController;
import com.biomed.smarttrak.vo.ProductExplorerReportVO;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.http.CookieUtil;
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
	private static final String SEL_NODES = "selNodes";
	private static final int CONTAINS_SEARCH = 1;
	private static final int BEGIN_SEARCH = 2;
	private static final int EXACT_SEARCH = 3;
	public static final String PE_STATE_COOKIE = "PEStateCookie";

	/**
	 * The default list of excluded columns in the product explorer and its report
	 * This is used when the user has not edited thier viewed columns during this session.
	 */
	private static final String DEFAULT_COLUMNS = "3|0|d|a";


	private enum BuildType {
		EXPORT, SAVE, SHARE, DELETE, HIERARCHY
	}

	/**
	 * Keep track of all solr compliant form fields and thier
	 * solr equivalents
	 */
	private enum SearchField {
		PRODUCT(CONTAINS_SEARCH, SearchDocumentHandler.TITLE, "Product Name", true),
		STATE(BEGIN_SEARCH, "search_state_s", "State", false),
		COUNTRY(EXACT_SEARCH, SearchDocumentHandler.COUNTRY, "Country", false),
		COMPANY(CONTAINS_SEARCH, "companysearch_s", "Company Name", false),
		SEGMENT(EXACT_SEARCH, SearchDocumentHandler.SECTION, "Segment", false),
		MARKET(EXACT_SEARCH, "target_market_ss", "Target Market", false),
		INDICATION(EXACT_SEARCH, "indication_ss", "Indication", false),
		TECH(EXACT_SEARCH, "technology_ss", "Technology", false),
		APPROACH(EXACT_SEARCH, "approach_ss", "Approach", false),
		CLASSIFICATION(EXACT_SEARCH, "classification_ss", "Classification", false),
		INTREG(EXACT_SEARCH, "intregionnm_ss", "International Region", false),
		INTPATH(EXACT_SEARCH, "intpathnm_ss", "International Path", false),
		INTSTATUS(EXACT_SEARCH, "intstatusnm_ss", "International Status", false),
		USPATH(EXACT_SEARCH, "uspathnm_ss", "US Path", false),
		USSTATUS(EXACT_SEARCH, "usstatusnm_ss", "US Status", false),
		ALLY(CONTAINS_SEARCH, "allysearch_ss", "Ally", false),
		OWNERSHIP(EXACT_SEARCH, "ownership_s", "Owner", false),
		ID(EXACT_SEARCH, SearchDocumentHandler.DOCUMENT_ID, "Product Id", false);

		private int searchType;
		private String solrField;
		private String fieldName;
		private boolean querySearch;

		SearchField(int searchType, String solrField, String fieldName, boolean querySearch) {
			this.searchType = searchType;
			this.solrField = solrField;
			this.fieldName = fieldName;
			this.querySearch = querySearch;
		}

		public String getSolrField() {
			return solrField;
		}

		public String getFieldname() {
			return fieldName;
		}

		public int getSearchType() {
			return searchType;
		}
		
		public boolean isQuerySearch() {
			return querySearch;
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


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SecurityController.isPeAuth(req);

		//Fast fail on the populateTable request as that's a json call.  No Redirects.
		if(!req.hasParameter("populateTable") && isRedirectRequest(req)) {
			return;
		}

		putModuleData(retrieveProducts(req, false));

		if (req.getSession().getAttribute(SAVED_QUERIES) == null)
			retrieveSavedQueries(req);
	}

	/**
	 * Check the request to determine if we need to redirect.
	 * @param req
	 * @return
	 */
	private boolean isRedirectRequest(ActionRequest req) {

		//Get the Cookie Value and set a base Url.
		String cookieVal = CookieUtil.getValue(PE_STATE_COOKIE, req.getCookies());
		String url = StringUtil.join(req.getRequestURL().toString(), "?");

		//Check for reset as a baseline.  If cookie Redirect is chosen, will update.
		boolean isRedirect = !StringUtil.isEmpty(cookieVal);

		//Check if we need to append Cookie Value to the redirect request.
		if(isRedirect && !url.contains("json")) {
			SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
			url = buildRedirectPath(url, cookieVal, SecurityController.isManageTool(site));
			CookieUtil.remove(req, PE_STATE_COOKIE);
			super.sendRedirect(url, null, req);
		} else {
			isRedirect = false;
		}

		return isRedirect;
	}

	/**
	 * Build the Redirect path.  Checks for missing params if we're in the
	 * manage tool that are required from that side (Essentially upgrades a
	 * public formatted cookie to an manage valid cookie).
	 * @param baseUrl
	 * @param cookieVal
	 * @param isManageTool
	 * @return
	 */
	private String buildRedirectPath(String baseUrl, String cookieVal, boolean isManageTool) {
		if(isManageTool) {
			if(!cookieVal.contains("actionType")) {
				cookieVal = StringUtil.join(cookieVal, "&actionType=tools");
			}

			if(!cookieVal.contains("facadeType")) {
				cookieVal = StringUtil.join(cookieVal, "&facadeType=explorer&load=true");
			}

			if(!cookieVal.contains("load")) {
				cookieVal = StringUtil.join(cookieVal, "load=true");
			}
		}
		return StringUtil.join(baseUrl, cookieVal);
	}

	/**
	 * Build a text representation of the filters applied to the search
	 */
	private void buildFilterList(ActionRequest req) {
		StringBuilder text = new StringBuilder(512);
		buildQueryFilters(text, req);
		if (req.hasParameter(SEL_NODES)) {
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
		String nodes = req.getParameter(SEL_NODES);
		for (String s : nodes.split(",")) {
			Node n = t.findNode(s);
			if (n == null || n.getDepthLevel() == 2) continue;
			if (part.length() > 2) {
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
		UserVO user = (UserVO) req.getSession().getAttribute(Constants.USER_DATA);
		StringBuilder sql = new StringBuilder(125);
		sql.append("SELECT * FROM ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_EXPLORER_QUERY WHERE USER_ID = ? order by lower(QUERY_NM) asc ");
		log.debug(sql);

		List<Map<String, String>> queries = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
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
		if (req.hasParameter(SEL_NODES)) buildNodeParams(req, qData);

		addFacetFields(req, qData);
		SolrQueryProcessor sqp = new SolrQueryProcessor(attributes, qData.getSolrCollectionPath());
		sqp.setFacetLimit(1500); //set this high enough so we can get all the companies.  ~1050 as of 02.12.2018 -JM
		SolrResponseVO vo = sqp.processQuery(qData);

		// Check to see if all the results should be returned instead of the current page
		if (getAll)
			getRemainingDocuments(vo, sqp, qData);

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
		List<SolrDocument> docs = new ArrayList<>(Convert.formatInteger(""+vo.getTotalResponses()));
		docs.addAll(vo.getResultDocuments());
		qData.setNumberResponses(1000);

		while (docs.size() < vo.getTotalResponses()) {
			qData.setStartLocation(docs.size());
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
		String nodes = req.getParameter(SEL_NODES);
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
			if (search.isQuerySearch()) {
				qData.setSearchData(buildQueryValue(req.getParameter(name), search, qData.getSearchData()));
			} else {
				String value = buildValues(req.getParameterValues(name), search.getSearchType());
				qData.addSolrField(new SolrFieldVO(FieldType.FILTER, search.getSolrField(), value, BooleanType.AND));
			}
		}
	}


	/**
	 * Create a searchData value that can be used with fields whose values can contain 
	 * problematic end characters that do not play nice with filter queries
	 * @param value
	 * @param search
	 * @param searchData
	 * @return
	 */
	private String buildQueryValue(String value, SearchField search, String searchData) {
		if (StringUtil.isEmpty(value)) return searchData;
		StringBuilder queryValue = new StringBuilder(StringUtil.isEmpty(searchData)? "":searchData);
		// Default search case that handles odd situations like ending with a +.
		queryValue.append(" ").append(search.solrField).append(":").append(value);
		// Standard search case
		queryValue.append(" ").append(search.solrField).append(":").append(getSearchValue(value, search.getSearchType()));
		return queryValue.toString();
	}

	/**
	 * Build a solr compatible value field out of the supplied value array
	 * @param parameterValues
	 * @param contains
	 * @return
	 */
	protected String buildValues(String[] parameterValues, int searchType) {
		StringBuilder value = new StringBuilder(50);
		value.append("(");
		for (String s : parameterValues) {
			if (value.length() > 2) value.append(" or ");
			value.append(getSearchValue(s, searchType));
		}
		value.append(")");

		return value.toString();
	}


	/**
	 * Create a solr search term from the supplied term.
	 */
	protected String getSearchValue(String searchTerm, int searchType) {
		StringBuilder finalTerm = new StringBuilder(searchTerm.length() + 2);

		String editedTerm = StringEscapeUtils.unescapeHtml(searchTerm).replace("(", "\\(").replace(")", "\\)");

		switch (searchType) {
			case CONTAINS_SEARCH:
				finalTerm.append("*").append(editedTerm.toLowerCase()).append("*");
				break;
			case BEGIN_SEARCH:
				finalTerm.append(editedTerm.toLowerCase()).append("*");
				break;
			case EXACT_SEARCH:
				finalTerm.append("\"").append(editedTerm).append("\"");
				break;
			default:
				//ignore unmapped values
		}

		return finalTerm.toString();
	}


	/**
	 * Get the solr information and set the standard parameters
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected SolrActionVO buildSolrAction(ActionRequest req, int start) throws ActionException {
		SmarttrakSolrAction sa = new SmarttrakSolrAction(actionInit);
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
		
		if (StringUtil.isEmpty(qData.getOrganizationId())) {
			SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
			qData.setOrganizationId(site.getOrganizationId());
		}

		sa.includeRoleACL(req, qData);

		return qData;
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
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
	@SuppressWarnings("unchecked")
	private void getHierarchy(ActionRequest req) throws ActionException {
		Set<String> allowedSections = getPopulatedHierarchies(req);

		SectionHierarchyAction sha = new SectionHierarchyAction();
		sha.setAttributes(getAttributes());
		sha.setDBConnection(getDBConnection());
		sha.retrieve(req);
		ModuleVO mod = (ModuleVO) sha.getAttribute(Constants.MODULE_DATA);
		List<Node> sections = (List<Node>) mod.getActionData();

		putModuleData(filterTree(allowedSections, sections));
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
			filterNode(allowedSections, n, sections);
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
	private void filterNode(Set<String> allowedSections, Node n, List<Node> sections) {
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
		Map<String, Object> data = new HashMap<>();
		data.put("data", retrieveProducts(req, true).getResultDocuments());
		if (req.getCookie("explorercolumns") != null) {
			data.put("columns", req.getCookie("explorercolumns").getValue());
		} else {
			data.put("columns", DEFAULT_COLUMNS);
		}
		report.setData(data);
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
			msg.setSubject("SmartTRAK Products");
			msg.addRecipient(req.getParameter("recipient"));
			msg.setFrom("info@smarttrak.com");

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
	protected void saveQuery(ActionRequest req) throws ActionException {
		boolean isUpdate = req.hasParameter("queryId");
		String sql = buildSaveQuery(isUpdate);
		
		Map<String, String> entry = new HashMap<>();
		if(isUpdate) entry.put("id", req.getParameter("queryId"));
		else entry.put("id", new UUIDGenerator().getUUID());
		entry.put("name", req.getParameter("queryName"));
		entry.put("url", buildUrl(req));
		entry.put("isNew", isUpdate ? "false" : "true");
		
		int index = 1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			UserVO user = (UserVO) req.getSession().getAttribute(Constants.USER_DATA);
			if(!isUpdate) ps.setString(index++, entry.get("id"));
			ps.setString(index++, user.getUserId());
			ps.setString(index++, entry.get("name"));
			ps.setString(index++, entry.get("url"));
			ps.setTimestamp(index++, Convert.getCurrentTimestamp());
			if(isUpdate) ps.setString(index++, entry.get("id"));

			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}

		super.putModuleData(entry);
		storeSessionQuery(req, entry, isUpdate);
	}
	
	/**
	 * Handles storing the retrieved query data out to session
	 * @param req
	 * @param entry
	 * @param isUpdate
	 */
	@SuppressWarnings("unchecked")
	protected void storeSessionQuery(ActionRequest req, Map<String, String> entry, boolean isUpdate) {
		if (req.getSession().getAttribute(SAVED_QUERIES) == null)
			req.getSession().setAttribute(SAVED_QUERIES, new ArrayList<Map<String, String>>());
		
		if(isUpdate) {// update the existing map entry
			List<Map<String, String>> entryList = ((List<Map<String, String>>)req.getSession().getAttribute(SAVED_QUERIES));
			Map<String, String> tempMap;
			for (int i = 0; i < entryList.size(); i++) {
				tempMap = entryList.get(i);
				if(tempMap.containsValue(entry.get("id"))) {
					entryList.remove(i);
					entryList.add(i, entry);
					break;
				}
			}
		}else {
			/*
			 * Flush out SAVED_QUERIES.  Will re-load them on next call.  Query
			 * will ensure proper ordering of newly added Query.
			 */
			req.getSession().removeAttribute(SAVED_QUERIES);
		}
	}
	
	/**
	 * Builds the appropriate explorer scenario query for either insert or update
	 * @param isUpdate
	 * @return
	 */
	protected String buildSaveQuery(boolean isUpdate) {
		StringBuilder sql = new StringBuilder(200);

		if(isUpdate) {
			sql.append("UPDATE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sql.append("BIOMEDGPS_EXPLORER_QUERY SET USER_ID =?, QUERY_NM =?, QUERY_TXT =?, ");
			sql.append("UPDATE_DT =? WHERE EXPLORER_QUERY_ID =? ");
		}else {
			sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sql.append("BIOMEDGPS_EXPLORER_QUERY (EXPLORER_QUERY_ID, USER_ID, QUERY_NM, QUERY_TXT, CREATE_DT) ");
			sql.append("VALUES(?,?,?,?,?)");
		}
		
		log.debug("Save query sql: " + sql);
		return sql.toString();
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
		url.append("&selNodes=").append(StringUtil.checkVal(req.getParameter(SEL_NODES)));

		return url.toString();
	}
}
