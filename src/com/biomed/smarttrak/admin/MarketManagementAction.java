package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.vo.MarketAttributeTypeVO;
import com.biomed.smarttrak.vo.MarketAttributeVO;
import com.biomed.smarttrak.vo.MarketVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SimpleActionAdapter;
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
 * <b>Changes: </b>
 ****************************************************************************/

public class MarketManagementAction extends SimpleActionAdapter {

	public static final String ACTION_TARGET = "actionTarget";
	
	private enum ActionTarget {
		MARKET, MARKETATTRIBUTE, ATTRIBUTE, SECTION
	}

	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
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
				retireveMarketAttributes(req);
				break;
			case ATTRIBUTE:
				retrieveAttributes(req);
				break;
			case SECTION:
				retrieveSections(req);
				break;
		}
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
		req.setParameter("getList", "true");
		retrieveAllAttributes(req);
	}


	/**
	 * Determine whether to get one market or all and do so.
	 * @param req
	 * @throws ActionException
	 */
	private void retrieveMarket(ActionRequest req) throws ActionException {
		if (req.hasParameter("marketId") && ! req.hasParameter("add")) {
			retrieveMarket(req.getParameter("marketId"));
		} else if (!req.hasParameter("add")) {
			retrieveMarkets(req);
		}
	}


	/**
	 * get a particular market attribute from the database for editing
	 * @param req
	 */
	private void retrieveMarketAttribute(ActionRequest req) {
		StringBuilder sql = new StringBuilder(300);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT xr.*, a.TYPE_CD, m.MARKET_NM FROM ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE_XR xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE a ");
		sql.append("ON a.ATTRIBUTE_ID = xr.ATTRIBUTE_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_MARKET m ");
		sql.append("ON xr.MARKET_ID = m.MARKET_ID ");
		sql.append("WHERE MARKET_ATTRIBUTE_ID = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(req.getParameter("marketAttributeId"));
		DBProcessor db = new DBProcessor(dbConn);
		MarketAttributeVO attr = (MarketAttributeVO) db.executeSelect(sql.toString(), params, new MarketAttributeVO()).get(0);
		super.putModuleData(attr);
		req.setParameter("rootNode", attr.getAttributeId());
	}
	
	
	/**
	 *Retrieve all attributes available to the market.
	 * @param req
	 */
	private void retrieveAllAttributes(ActionRequest req) {
		StringBuilder sql = new StringBuilder(100);
		List<Object> params = new ArrayList<>();
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_MARKET_ATTRIBUTE ");
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

		int rpp = Convert.formatInteger(req.getParameter("rpp"), 10);
		int page = Convert.formatInteger(req.getParameter("page"), 0);
		int end = orderedResults.size() < rpp*(page+1)? orderedResults.size() : rpp*(page+1);
		
		// If all attributes of a type is being requested set it as a request attribute since it is
		// being used to supplement the attribute xr editing.
		// Search data should not be turned into a tree after a search as requisite nodes may be missing
		if (req.hasParameter("attributeTypeCd") || Convert.formatBoolean(req.getParameter("getList"))) {
			Tree t = new Tree(orderedResults);
			Node rootNode = null;
			if (req.hasParameter("rootNode")) {
				rootNode = getTopParent(t, t.findNode(req.getParameter("rootNode")));
			}
			
			if (rootNode != null && rootNode.getNumberChildren() > 0) {
				req.getSession().setAttribute("attributeList", t.preorderList(rootNode));
			} else {
				req.getSession().setAttribute("attributeList", t.preorderList());
			}

		} else if (req.hasParameter("searchData")) {
			super.putModuleData(orderedResults.subList(rpp*page, end), orderedResults.size(), false);
		} else {
			super.putModuleData(new Tree(orderedResults).getPreorderList().subList(rpp*page, end), orderedResults.size(), false);
		}
	}


	/**
	 * Ensure that we are getting a top level root node.
	 * @param t
	 * @param rootNode
	 * @return
	 */
	protected Node getTopParent(Tree t, Node rootNode) {
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
		StringBuilder sql = new StringBuilder(100);
		List<Object> params = new ArrayList<>();
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_MARKET_ATTRIBUTE ");
		sql.append("WHERE ATTRIBUTE_ID = ? ");
		params.add(attributeId);
		log.debug(sql);
		DBProcessor db = new DBProcessor(dbConn);
		List<Object> res = db.executeSelect(sql.toString(), params, new MarketAttributeTypeVO());

		if (!res.isEmpty()) {
			MarketAttributeTypeVO attr = (MarketAttributeTypeVO) db.executeSelect(sql.toString(), params, new MarketAttributeTypeVO()).get(0);
			super.putModuleData(attr);
		} else {
			super.putModuleData(new MarketAttributeTypeVO());
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
		String customDb = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		sql.append("select * ").append("FROM ").append(customDb).append("BIOMEDGPS_MARKET ");
		
		// If the request has search terms on it add them here
		if (req.hasParameter("searchData")) {
			sql.append("WHERE lower(MARKET_NM) like ?");
			params.add("%" + req.getParameter("searchData").toLowerCase() + "%");
		}
		log.debug(sql);
		int rpp = Convert.formatInteger(req.getParameter("rpp"), 10);
		int page = Convert.formatInteger(req.getParameter("page"), 0);
		
		DBProcessor db = new DBProcessor(dbConn);
		List<Object> markets = db.executeSelect(sql.toString(), params, new MarketVO());
		int end = markets.size() < rpp*(page+1)? markets.size() : rpp*(page+1);
		super.putModuleData(markets.subList(rpp*page, end), markets.size(), false);
	}

	
	/**
	 * Get all information related to the supplied market.
	 * @param marketId
	 * @throws ActionException
	 */
	protected void retrieveMarket(String marketId) throws ActionException {
		MarketVO market;
		StringBuilder sql = new StringBuilder(100);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_MARKET ");
		sql.append("WHERE MARKET_ID = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(marketId);
		DBProcessor db = new DBProcessor(dbConn);
		market = (MarketVO) db.executeSelect(sql.toString(), params, new MarketVO()).get(0);

		// Get specifics on market details
		addAttributes(market);
		addSections(market);
		
		super.putModuleData(market);
	}
	
	
	/**
	 * Get all attributes associated with the supplied market.
	 * @param market
	 */
	protected void addAttributes(MarketVO market) {
		List<Object> results = getMarketAttributes(market.getMarketId());
		for (Object o : results) {
			market.addAttribute((MarketAttributeVO)o);
		}
	}
	
	
	/**
	 * Get all the sections that are associated with the supplied market
	 * @param market
	 * @throws ActionException
	 */
	protected void addSections(MarketVO market) throws ActionException {
		StringBuilder sql = new StringBuilder(275);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT SECTION_NM, xr.MARKET_SECTION_XR_ID FROM ").append(customDb).append("BIOMEDGPS_MARKET_SECTION xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_SECTION s ");
		sql.append("ON s.SECTION_ID = xr.SECTION_ID ");
		sql.append("WHERE MARKET_ID = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, market.getMarketId());
			
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				market.addSection(new GenericVO(rs.getString("MARKET_SECTION_XR_ID"), rs.getString("SECTION_NM")));
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
		ContentHierarchyAction c = new ContentHierarchyAction();
		c.setActionInit(actionInit);
		c.setAttributes(attributes);
		c.setDBConnection(dbConn);
		
		List<Node> hierarchy = new Tree(c.getHierarchy(null)).preorderList();
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
		sql.append("SELECT SECTION_ID FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_MARKET_SECTION WHERE MARKET_ID = ? ");
		
		List<String> activeSections = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, marketId);
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				activeSections.add(rs.getString("SECTION_ID"));
			}
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
	protected List<Object> getMarketAttributes(String marketId) {
		StringBuilder sql = new StringBuilder(150);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE_XR xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE a ");
		sql.append("ON a.ATTRIBUTE_ID = xr.ATTRIBUTE_ID ");
		sql.append("WHERE MARKET_ID = ? ");
		log.debug(sql+"|"+marketId);
		List<Object> params = new ArrayList<>();
		params.add(marketId);
		DBProcessor db = new DBProcessor(dbConn);
		
		// DBProcessor returns a list of objects that need to be individually cast to attributes
		return db.executeSelect(sql.toString(), params, new MarketAttributeVO());
	}

	
	/**
	 * Update a market or related attribute of a market
	 * @param req
	 * @throws ActionException
	 */
	protected void updateElement(ActionRequest req) throws ActionException {
		ActionTarget action = ActionTarget.valueOf(req.getParameter(ACTION_TARGET));
		DBProcessor db = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
		switch(action) {
			case MARKET:
				MarketVO c = new MarketVO(req);
				saveMarket(c, db);
				break;
			case MARKETATTRIBUTE:
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
		}
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
		deleteSection(true, req.getParameter("marketId"));
		
		StringBuilder sql = new StringBuilder(225);
		sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_MARKET_SECTION (MARKET_SECTION_XR_ID, SECTION_ID, ");
		sql.append("MARKET_ID, CREATE_DT) ");
		sql.append("VALUES(?,?,?,?) ");
		String marketId = req.getParameter("marketId");
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
		DBProcessor db = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
		try {
			switch(action) {
				case MARKET:
					MarketVO c = new MarketVO(req);
					db.delete(c);
					break;
				case MARKETATTRIBUTE:
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
		sql.append("DELETE FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
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
	
	
	/**
	 * Take in front end requests and direct them to the proper delete or update method
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		String buildAction = req.getParameter("buildAction");
		String msg = StringUtil.capitalizePhrase(buildAction) + " completed successfully.";
		try {
			if ("update".equals(buildAction)) {			
				updateElement(req);
			} else if("delete".equals(buildAction)) {
				deleteElement(req);
			}
		} catch (Exception e) {
			msg = StringUtil.capitalizePhrase(buildAction) + " failed to complete successfully. Please contact an administrator for assistance";
		}
		
		redirectRequest(msg, buildAction, req);
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
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}
}
