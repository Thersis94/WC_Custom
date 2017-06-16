package com.biomed.smarttrak.admin;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.action.AdminControllerAction.Status;
import com.biomed.smarttrak.action.MarketAction;
import com.biomed.smarttrak.util.MarketIndexer;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.MarketAttributeTypeVO;
import com.biomed.smarttrak.vo.MarketAttributeVO;
import com.biomed.smarttrak.vo.MarketVO;
import com.biomed.smarttrak.vo.SectionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: MarketManagementAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Handles managing markets for BiomedGPS' SmartTrak system
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Feb 8, 2017<p/>
 * <b>Changes: 
 * Due to changes in how markets deal with sections the default operation now 
 * uses only a single section per market. Old code that permits multiple
 * sections per market has be left alone should it ever be needed
 * - Eric Damschroder 3/10/2017
 * </b>
 ****************************************************************************/
public class MarketManagementAction extends AuthorAction {

	public static final String ACTION_TARGET = "actionTarget";
	public static final String GRAPH_ID = "GRID";
	public static final String CONTENT_ATTRIBUTE_ID = "CONTENT";

	private enum ActionTarget {
		MARKET, MARKETATTRIBUTE, ATTRIBUTE, SECTION, MARKETGRAPH, MARKETLINK,MARKETATTACH, PREVIEW
	}
	
	/**
	 * Content that can be autogenerated on
	 * creation of a new market
	 */
	private enum ContentType {
		OVERVIEW("Market Overview", 1),
		INCIDENCE("Incidence & Prevalence", 2),
		CONDITIONS("Conditions", 3),
		TECHNOLOGIES("Technologies", 4),
		COMPETITORS("Competitors", 5),
		REGULATORY("Regulatory", 6),
		REMIBURSEMENT("Reimbursement", 7);
		
		private String contentName;
		private int order;
		
		private ContentType(String contentName, int order) {
			this.contentName = contentName;
			this.order = order;
		}
		
		public String getContentName() {
			return contentName;
		}
		
		public int getOrder() {
			return order;
		}
		
		public static ContentType getFromString(String contentType) {
			if (StringUtil.isEmpty(contentType)) return null;
			try {
				return ContentType.valueOf(contentType);
			} catch (Exception e) {
				log.error("Error getting content type: ", e);
				return null;
			}
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


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (req.hasParameter("buildAction")) {
			super.retrieve(req);
			return;
		}
		
		ActionTarget action;
		if (req.hasParameter(ACTION_TARGET)) {
			action = ActionTarget.valueOf(req.getParameter(ACTION_TARGET));
		} else {
			action = ActionTarget.MARKET;
		}

		switch (action) {
			case MARKET:
				retrieveMarket(req);
				break;
			case MARKETATTRIBUTE:
			case MARKETGRAPH:
			case MARKETLINK:
			case MARKETATTACH:
				retireveMarketAttributes(req);
				break;
			case ATTRIBUTE:
				retrieveAttributes(req);
				break;
			case SECTION:
				retrieveSections(req);
				break;
			case PREVIEW :
				retrievePreview(req);
				break;
		}
	}
	
	
	/**
	 * Get the market as it would appear on the public side.
	 * @param req
	 * @throws ActionException
	 */
	protected void retrievePreview(ActionRequest req) throws ActionException {
		MarketAction ma = new MarketAction(actionInit);
		ma.setDBConnection(dbConn);
		ma.setAttributes(attributes);
		super.putModuleData(ma.retrieveFromDB(req.getParameter("marketId"), req, true));
	}
	


	/**
	 * Determine which attributes need to be retrieved and do so.
	 * @param req
	 */
	private void retrieveAttributes(ActionRequest req) {
		if (req.hasParameter("attributeId")) {
			retrieveAttribute(req.getParameter("attributeId"));
		} else if (!req.hasParameter("add")) {
			retrieveAllAttributes(req);
		}
	}


	/**
	 * Get attributes assigned to a particular market.
	 * @param req
	 */
	private void retireveMarketAttributes(ActionRequest req) {
		if (req.hasParameter("marketAttributeId"))
			retrieveMarketAttribute(req);
	}


	/**
	 * Determine whether to get one market or all and do so.
	 * @param req
	 * @throws ActionException
	 */
	private void retrieveMarket(ActionRequest req) throws ActionException {
		if (req.hasParameter("marketId") && ! req.hasParameter("add")) {
			retrieveSingleMarket(req);

			req.getSession().setAttribute("marketSections", loadDefaultTree().preorderList());
		} else if (!req.hasParameter("add")) {
			retrieveMarkets(req);
		} else{ 
			loadAuthors(req); //load list of BiomedGPS Staff for the "Author" drop-down
			if (req.getSession().getAttributes().keySet().contains("hierarchyTree")){
				// This is a form for a new market make sure that the hierarchy tree is present 
				req.getSession().setAttribute("marketSections", loadDefaultTree().preorderList());
			}
		}
	}


	/**
	 * get a particular market attribute from the database for editing
	 * @param req
	 */
	private void retrieveMarketAttribute(ActionRequest req) {
		StringBuilder sql = new StringBuilder(300);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT xr.*, a.TYPE_CD, m.MARKET_NM FROM ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE_XR xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE a ");
		sql.append("ON a.ATTRIBUTE_ID = xr.ATTRIBUTE_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_MARKET m ");
		sql.append("ON xr.MARKET_ID = m.MARKET_ID ");
		sql.append("WHERE MARKET_ATTRIBUTE_ID = ? ");

		List<Object> params = new ArrayList<>();
		params.add(req.getParameter("marketAttributeId"));
		DBProcessor db = new DBProcessor(dbConn, customDb);
		MarketAttributeVO attr = (MarketAttributeVO) db.executeSelect(sql.toString(), params, new MarketAttributeVO()).get(0);
		putModuleData(attr);
		req.setParameter("rootNode", attr.getAttributeId());
	}


	/**
	 *Retrieve all attributes available to the market.
	 * @param req
	 */
	private void retrieveAllAttributes(ActionRequest req) {
		StringBuilder sql = new StringBuilder(100);
		List<Object> params = new ArrayList<>();
		sql.append("SELECT * FROM ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_MARKET_ATTRIBUTE ");
		if (req.hasParameter("searchData")) {
			sql.append("WHERE lower(ATTRIBUTE_NM) like ? ");
			params.add("%" + req.getParameter("searchData").toLowerCase() + "%");
		}
		if (req.hasParameter("attributeTypeCd")) {
			sql.append("WHERE TYPE_CD = ? ");
			params.add(req.getParameter("attributeTypeCd"));
		}
		sql.append("ORDER BY ORDER_NO ");
		log.debug(sql);
		
		DBProcessor db = new DBProcessor(dbConn);
		List<Object> results = db.executeSelect(sql.toString(), params, new MarketAttributeTypeVO());

		List<Node> orderedResults = new ArrayList<>();
		for (Object o : results) {
			MarketAttributeTypeVO attr = (MarketAttributeTypeVO)o;
			Node n = new Node(attr.getAttributeId(), attr.getParentId());
			n.setUserObject(attr);
			orderedResults.add(n);
		}
		setAttributeData(orderedResults, req);
	}


	/**
	 * Determine how the supplied results should be added to the module data.
	 * @param orderedResults
	 * @param req
	 */
	private void setAttributeData(List<Node> orderedResults, ActionRequest req) {
		// If all attributes of a type is being requested set it as a request attribute since it is
		// being used to supplement the attribute xr editing.
		// Search data should not be turned into a tree after a search as requisite nodes may be missing
		if (req.hasParameter("searchData")) {
			putModuleData(orderedResults, orderedResults.size(), false);
		} else {
			putModuleData(new SmarttrakTree(orderedResults).getPreorderList(), orderedResults.size(), false);
		}
	}


	/**
	 * Ensure that we are getting a top level root node.
	 * @param t
	 * @param rootNode
	 * @return
	 */
	protected Node getTopParent(SmarttrakTree t, Node rootNode) {
		if (rootNode != null && rootNode.getParentId() != null) {
			return getTopParent(t, t.findNode(rootNode.getParentId()));
		}
		return rootNode;
	}


	/**
	 * Get the details of the supplied attribute type
	 * @param attributeId
	 */
	protected void retrieveAttribute(String attributeId) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		List<Object> params = new ArrayList<>();
		sql.append("SELECT * FROM ").append(schema).append("BIOMEDGPS_MARKET_ATTRIBUTE ");
		sql.append("WHERE ATTRIBUTE_ID = ? ");
		params.add(attributeId);
		log.debug(sql);
		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object> res = db.executeSelect(sql.toString(), params, new MarketAttributeTypeVO());

		if (!res.isEmpty()) {
			MarketAttributeTypeVO attr = (MarketAttributeTypeVO) db.executeSelect(sql.toString(), params, new MarketAttributeTypeVO()).get(0);
			putModuleData(attr);
		} else {
			putModuleData(new MarketAttributeTypeVO());
		}
	}


	/**
	 * Retrieve all companies from the database as well as create a flag that 
	 * shows whether the market has been invested in by another market.
	 * @param req
	 * @throws ActionException
	 */
	protected void retrieveMarkets(ActionRequest req) throws ActionException {
		List<Object> params = new ArrayList<>();
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		sql.append("select * FROM ").append(customDb).append("BIOMEDGPS_MARKET m ");
		sql.append("LEFT JOIN COUNTRY c on c.COUNTRY_CD = m.REGION_CD ");
		sql.append("WHERE 1=1 ");

		// If the request has search terms on it add them here
		if (req.hasParameter("searchData")) {
			sql.append("and lower(MARKET_NM) like ?");
			params.add("%" + req.getParameter("searchData").toLowerCase() + "%");
		}
		
		if (StringUtil.isEmpty(req.getParameter("inactive"))) {
			sql.append("and (m.STATUS_NO = '").append(Status.P.toString()).append("' ");
			sql.append("or m.STATUS_NO = '").append(Status.E.toString()).append("') ");
		}
		
		sql.append("ORDER BY MARKET_NM ");
		log.debug(sql);

		DBProcessor db = new DBProcessor(dbConn, customDb);
		List<Object> markets = db.executeSelect(sql.toString(), params, new MarketVO());
		putModuleData(markets, markets.size(), false);
	}


	/**
	 * Get all information related to the supplied market.
	 * @param marketId
	 * @throws ActionException
	 */
	protected void retrieveSingleMarket(ActionRequest req) throws ActionException {
		MarketVO market;
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		sql.append("SELECT * FROM ").append(schema).append("BIOMEDGPS_MARKET m ");
		sql.append("WHERE MARKET_ID = ? ");
		List<Object> params = new ArrayList<>();
		params.add(req.getParameter("marketId"));
		
		DBProcessor db = new DBProcessor(dbConn, schema);
		market = (MarketVO) db.executeSelect(sql.toString(), params, new MarketVO()).get(0);
		req.getSession().setAttribute("marketName", market.getMarketName());
		
		// Get specifics on market details
		addAttributes(market, req.getParameter("typeCd"));
		addSections(market);
		loadAuthors(req); //load list of BiomedGPS Staff for the "Author" drop-down
		putModuleData(market);
	}


	/**
	 * Get all attributes associated with the supplied market.
	 * @param market
	 * @throws ActionException 
	 */
	protected void addAttributes(MarketVO market, String typeCd) throws ActionException {
		List<Object> results = getMarketAttributes(market.getMarketId(), typeCd);

		for (Object o : results) {
			MarketAttributeVO m = (MarketAttributeVO)o;
			market.addMarketAttribute(m);
		}
	}


	/**
	 * Markets are identified by a single section and do not support multiples
	 * @param market
	 * @throws ActionException
	 */
	protected void addSections(MarketVO market) throws ActionException {
		StringBuilder sql = new StringBuilder(275);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT SECTION_NM, xr.MARKET_SECTION_XR_ID, xr.SECTION_ID FROM ").append(customDb).append("BIOMEDGPS_MARKET_SECTION xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_SECTION s ");
		sql.append("ON s.SECTION_ID = xr.SECTION_ID ");
		sql.append("WHERE MARKET_ID = ? limit 1 ");

		Tree t = loadDefaultTree();
		t.buildNodePaths();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, market.getMarketId());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				market.setMarketSection(new SectionVO(rs));
			}

		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Get all sections available to companies and mark the active sections
	 * @param req
	 * @throws ActionException
	 */
	protected void retrieveSections(ActionRequest req) throws ActionException {
		SectionHierarchyAction c = new SectionHierarchyAction();
		c.setActionInit(actionInit);
		c.setAttributes(attributes);
		c.setDBConnection(dbConn);

		List<Node> hierarchy = new SmarttrakTree(c.getHierarchy()).preorderList();
		List<String> activeNodes = getActiveSections(req.getParameter("marketId"));

		// Loop over all sections and set the leaf property to 
		// signify it being in use by the current market.
		for (Node n : hierarchy) {
			if (activeNodes.contains(n.getNodeId())) {
				n.setLeaf(true);
			} else {
				n.setLeaf(false);
			}
		}
		super.putModuleData(hierarchy);
	}


	/**
	 * Gets all sections that have been assigned to the supplied market
	 * @param marketId
	 * @return
	 * @throws ActionException
	 */
	protected List<String> getActiveSections(String marketId) throws ActionException {
		StringBuilder sql = new StringBuilder(150);
		sql.append("SELECT SECTION_ID FROM ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_MARKET_SECTION WHERE MARKET_ID = ? ");

		List<String> activeSections = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, marketId);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				activeSections.add(rs.getString("SECTION_ID"));

		} catch (Exception e) {
			throw new ActionException(e);
		}

		return activeSections;
	}


	/**
	 * Returns a list of attributes for a market
	 * @param marketId
	 * @return
	 */
	protected List<Object> getMarketAttributes(String marketId, String typeCd) {
		List<Object> params = new ArrayList<>();
		params.add(marketId);
		StringBuilder sql = new StringBuilder(300);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT xr.*, a.*, g.TITLE_NM as GROUP_NM FROM ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE_XR xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE a ");
		sql.append("ON a.ATTRIBUTE_ID = xr.ATTRIBUTE_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_GRID g ");
		sql.append("ON g.GRID_ID = xr.VALUE_1_TXT ");
		sql.append("WHERE MARKET_ID = ? ");
		if (!StringUtil.isEmpty(typeCd)) {
			sql.append("and a.TYPE_CD = ? ");
			params.add(typeCd);
		}
		sql.append("ORDER BY xr.ORDER_NO ");
		log.debug(sql+"|"+marketId+"|"+typeCd);

		// DBProcessor returns a list of objects that need to be individually cast to attributes
		DBProcessor db = new DBProcessor(dbConn, customDb);
		return db.executeSelect(sql.toString(), params, new MarketAttributeVO());
	}


	/**
	 * Update a market or related attribute of a market
	 * @param req
	 * @throws ActionException
	 */
	protected String updateElement(ActionRequest req) throws ActionException {
		ActionTarget action = ActionTarget.valueOf(req.getParameter(ACTION_TARGET));
		DBProcessor db = new DBProcessor(dbConn, (String) getAttribute(Constants.CUSTOM_DB_SCHEMA));
		String marketId = req.getParameter("marketId");

		switch(action) {
			case MARKET:
				marketId = completeMarketSave(marketId, req, db);
				break;
			case MARKETATTRIBUTE:
			case MARKETGRAPH:
			case MARKETLINK:
			case MARKETATTACH:
				MarketAttributeVO attr = new MarketAttributeVO(req);
				saveAttribute(attr, db);
				break;
			case ATTRIBUTE:
				MarketAttributeTypeVO t = new MarketAttributeTypeVO(req);
				saveAttributeType(t, db, Convert.formatBoolean(req.getParameter("insert")));
				break;
			case SECTION:
				saveSections(req);
				break;
			default:break;
		}
		return marketId;
	}


	/**
	 * Generate empty content with titles based of the ContentType enum that
	 * have been selected by the user on market creation.
	 * @param req
	 * @param marketId
	 * @throws ActionException
	 */
	private void generateContent(ActionRequest req, String marketId) throws ActionException {
		String[] contentList = req.getParameterValues("contentName");
		// If nothing is supplied we can return without issue.
		if (contentList.length == 0) return;
		
		StringBuilder sql = new StringBuilder(275);
		sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_MARKET_ATTRIBUTE_XR(MARKET_ATTRIBUTE_ID, ATTRIBUTE_ID,  ");
		sql.append("MARKET_ID, TITLE_TXT, ORDER_NO, STATUS_NO, CREATE_DT) ");
		sql.append("VALUES(?,?,?,?,?,?,?)");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (String contentName : contentList) {
				ContentType type = ContentType.getFromString(contentName);
				if (type == null) continue;
				log.debug(type.getContentName());
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, CONTENT_ATTRIBUTE_ID);
				ps.setString(3, marketId);
				ps.setString(4, type.getContentName());
				ps.setInt(5, type.getOrder());
				ps.setString(6, Status.P.toString());
				ps.setTimestamp(7, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			
			ps.executeBatch();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Do a complete market save
	 * @param marketId
	 * @param req
	 * @param db
	 * @throws ActionException
	 */
	protected String completeMarketSave(String marketId, ActionRequest req, DBProcessor db) throws ActionException {
		MarketVO c = new MarketVO(req);
		boolean isInsert = StringUtil.isEmpty(c.getMarketId());
		saveMarket(c, db);
		if (isInsert) generateContent(req, c.getMarketId());
		// Market save also includes the single section associated
		// with this market
		req.setParameter("marketId", c.getMarketId());
		saveSections(req);
		return marketId;
	}


	/**
	 * Check whether the supplied attribute needs to be inserted or updated and do so.
	 * @param attr
	 * @param db
	 * @throws ActionException
	 */
	protected void saveAttribute(MarketAttributeVO attr, DBProcessor db) throws ActionException {
		try {
			if (StringUtil.isEmpty(attr.getMarketAttributeId())) {
				attr.setMarketAttributeId(new UUIDGenerator().getUUID());
				db.insert(attr);
			} else {
				db.update(attr);
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Check whether we need to insert or update the supplied vo and do so.
	 * Then update the investors for the market.
	 * @param c
	 * @param db
	 * @throws ActionException
	 */
	protected void saveMarket(MarketVO m, DBProcessor db) throws ActionException {
		try {
			if (StringUtil.isEmpty(m.getMarketId())) {
				m.setMarketId(new UUIDGenerator().getUUID());
				db.insert(m);
			} else {
				db.update(m);
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Check whether the supplied attribute type needs to be inserted or updated and do so.
	 * @param attr
	 * @param db
	 * @param boolean1 
	 * @throws ActionException
	 */
	protected void saveAttributeType(MarketAttributeTypeVO t, DBProcessor db, Boolean insert) throws ActionException {
		try {
			if (insert) {
				db.insert(t);
			} else {
				db.update(t);
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Add the supplied sections to the market xr table
	 * @param req
	 * @throws ActionException
	 */
	protected void saveSections(ActionRequest req) throws ActionException {
		// Delete all sections currently assigned to this market before adding
		// what is on the request object.
		String marketId = req.getParameter("marketId");
		deleteSection(true, marketId);
		
		// If nothing is there to add return now.
		if (!req.hasParameter("sectionId")) return;

		StringBuilder sql = new StringBuilder(225);
		sql.append("INSERT INTO ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_MARKET_SECTION (MARKET_SECTION_XR_ID, SECTION_ID, ");
		sql.append("MARKET_ID, CREATE_DT) ");
		sql.append("VALUES(?,?,?,?) ");
		log.debug(sql+"|"+req.getParameter("sectionId")+"|"+req.getParameter("marketId"));
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (String sectionId : req.getParameterValues("sectionId")) {
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, sectionId);
				ps.setString(3, marketId);
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Delete a supplied element
	 * @param req
	 * @throws ActionException
	 */
	protected void deleteElement(ActionRequest req) throws ActionException {
		ActionTarget action = ActionTarget.valueOf(req.getParameter(ACTION_TARGET));
		DBProcessor db = new DBProcessor(dbConn, (String) getAttribute(Constants.CUSTOM_DB_SCHEMA));
		try {
			switch(action) {
				case MARKET:
					MarketVO c = new MarketVO(req);
					db.delete(c);
					break;
				case MARKETATTRIBUTE:
				case MARKETGRAPH:
				case MARKETLINK:
				case MARKETATTACH:
					MarketAttributeVO attr = new MarketAttributeVO(req);
					db.delete(attr);
					break;
				case ATTRIBUTE:
					MarketAttributeTypeVO t = new MarketAttributeTypeVO(req);
					db.delete(t);
					break;
				case SECTION:
					deleteSection(false, req.getParameter("sectionId"));
					break;
				default:break;
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Delete section xrs for a market. Deletes come in single xr deletion and
	 * full wipes used when new xrs are being saved.
	 * @param full
	 * @param id
	 * @throws ActionException
	 */
	protected void deleteSection(boolean full, String id) throws ActionException {
		StringBuilder sql = new StringBuilder(150);
		sql.append("DELETE FROM ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_MARKET_SECTION WHERE ");
		if (full) {
			sql.append("MARKET_ID = ? ");
		} else {
			sql.append("MARKET_SECTION_XR_ID = ? ");
		}
		log.debug(sql+"|"+id);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}
	
	
	@Override
	public void delete(ActionRequest req) throws ActionException {
		deleteElement(req);
	}


	/*
	 * Take in front end requests and direct them to the proper delete or update method
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		String buildAction = req.getParameter("buildAction");
		String msg = StringUtil.capitalizePhrase(buildAction) + " completed successfully.";
		String marketId = null;
		try {
			if ("update".equals(buildAction)) {			
				marketId = updateElement(req);
			} else if ("delete".equals(buildAction)) {
				deleteElement(req);
			} else if ("orderUpdate".equals(buildAction)) {
				updateOrder(req);
				// We don't want to send redirects after an order update
				return;
			}
		} catch (Exception e) {
			log.error("Error attempting to build: ", e);
			msg = StringUtil.capitalizePhrase(buildAction) + " failed to complete successfully. Please contact an administrator for assistance";
		}

		if (!StringUtil.isEmpty(marketId)) {
			String status = req.getParameter("statusNo");
			if (StringUtil.isEmpty(status))
				status = findStatus(marketId);
			writeToSolr(marketId, status);
		}

		redirectRequest(msg, buildAction, req);
	}


	/**
	 * Alter the order of the supplied attribute
	 * @param req
	 * @throws ActionException
	 */
	protected void updateOrder(ActionRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(150);
		sql.append("UPDATE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_MARKET_ATTRIBUTE_XR SET ORDER_NO = ? WHERE MARKET_ATTRIBUTE_ID = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			String[] order = req.getParameterValues("orderNo");
			String[] ids = req.getParameterValues("marketAttributeId");
			for (int i=0; i < order.length || i < ids.length; i++) {
				ps.setInt(1, Convert.formatInteger(order[i]));
				ps.setString(2, ids[i]);
				ps.addBatch();
			}
			
			ps.executeBatch();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Get the status of the supplied market.
	 * @param marketId
	 * @return
	 * @throws ActionException
	 */
	private String findStatus(String marketId) throws ActionException {
		StringBuilder sql = new StringBuilder(125);
		sql.append("SELECT STATUS_NO from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_MARKET WHERE MARKET_ID = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, marketId);
			
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				return rs.getString("STATUS_NO");
			}
		} catch (SQLException e) {
			throw new ActionException(e);
		}

		// If we didn't find a market with this id the action was a delete as solr needs to recognize that
		return "D";
	}


	/**
	 * Save an UpdatesVO to solr.
	 * @param u
	 */
	protected void writeToSolr(String marketId, String status) {
		MarketIndexer idx = MarketIndexer.makeInstance(getAttributes());
		idx.setDBConnection(dbConn);

		//if status is archived or deleted, remove this market from Solr
		if ("A".equals(status) || "D".equals(status)) {
			try {
				idx.purgeSingleItem(marketId);
			} catch (IOException e) {
				log.warn("could not delete market from solr " + marketId, e);
			}
		} else {
			idx.addSingleItem(marketId);
		}
	}


	/**
	 * Build the redirect for build requests
	 * @param msg
	 * @param buildAction
	 * @param req
	 */
	protected void redirectRequest(String msg, String buildAction, ActionRequest req) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		// Redirect the user to the appropriate page
		StringBuilder url = new StringBuilder(128);
		url.append(page.getFullPath()).append("?actionType=marketAdmin&").append("msg=").append(msg);

		// Only add a tab parameter if one was provided.
		if (req.hasParameter("tab")) {
			url.append("&tab=").append(req.getParameter("tab"));
		}
		//if a market is being deleted do not redirect the user to a market page
		if (!"delete".equals(buildAction) || 
				ActionTarget.valueOf(req.getParameter(ACTION_TARGET)) != ActionTarget.MARKET) {
			url.append("&marketId=").append(req.getParameter("marketId"));
		}

		if ("ATTRIBUTE".equals(req.getParameter(ACTION_TARGET)))
			url.append("&").append(ACTION_TARGET).append("=ATTRIBUTE");

		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}

}