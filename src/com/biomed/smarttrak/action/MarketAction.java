package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.biomed.smarttrak.security.SecurityController;
import com.biomed.smarttrak.vo.MarketAttributeVO;
import com.biomed.smarttrak.vo.MarketVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: MarketAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Return either a list of markets by search terms
 * or details on a particular market.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Feb 15, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class MarketAction extends SBActionAdapter {
	private final static String DEFAULT_GROUP = "Other";
	
	public MarketAction() {
		super();
	}

	public MarketAction(ActionInitVO init) {
		super(init);
	}
	
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (req.hasParameter("reqParam_1")) {
			retrieveMarket(req.getParameter("reqParam_1"), req);
		} else {
			retrieveMarkets(req);
		}
	}

	
	/**
	 * Get the indicated market
	 * @param marketId
	 * @throws ActionException
	 */
	protected void retrieveMarket(String marketId, ActionRequest req) throws ActionException {
		DBProcessor db = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
		MarketVO market = new MarketVO();
		market.setMarketId(marketId);
		try {
			db.getByPrimaryKey(market);
			addAttributes(market);
			addSections(market);
		} catch (Exception e) {
			throw new ActionException(e);
		}
		
		//TODO turn this on once complete and ready for testing
		//verify user has access to this market
		//SecurityController.getInstance(req).isUserAuthorized(market, req);
		
		super.putModuleData(market);
	}

	
	/**
	 * Build the time since last updated message
	 * @param market
	 */
	private void buildUpdateMsg(MarketVO market) {
		// Unpublished markets can be skipped
		if (!"P".equals(market.getStatusNo())) {
			return;
		}
		long diff = Convert.getCurrentTimestamp().getTime() - market.getUpdateDt().getTime();
		long diffDays = diff / (1000 * 60 * 60 * 24);
		long diffHours = diff / (1000 * 60 * 60);
		if (diffDays > 365) {
			int years = (int) (diffDays/365);
			market.setUpdateMsg(years + " year(s) ago");
		} else if (diffDays > 30) {
			int months = (int) (diffDays/30);
			market.setUpdateMsg(months + " month(s) ago");
		} else if (diffDays > 7) {
			int weeks = (int) (diffDays/7);
			market.setUpdateMsg(weeks + " week(s) ago");
		} else if (diffDays > 0) {
			market.setUpdateMsg(diffDays + " day(s) ago");
		} else if (diffHours > 0) {
			market.setUpdateMsg(diffHours + " hour(s) ago");
		} else {
			market.setUpdateMsg("less than an hour ago");
		}
		
		
	}

	/**
	 * Get all attributes for the supplied market
	 * @param market
	 */
	protected void addAttributes(MarketVO market) {
		StringBuilder sql = new StringBuilder(150);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE_XR xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE a ");
		sql.append("ON a.ATTRIBUTE_ID = xr.ATTRIBUTE_ID ");
		sql.append("WHERE MARKET_ID = ? ");
		sql.append("ORDER BY xr.ORDER_NO");
		log.debug(sql+"|"+market.getMarketId());
		
		List<Object> params = new ArrayList<>();
		params.add(market.getMarketId());
		DBProcessor db = new DBProcessor(dbConn);
		
		List<Object> results = db.executeSelect(sql.toString(), params, new MarketAttributeVO());
		
		for (Object o : results) {
			market.addMarketAttribute((MarketAttributeVO)o);
		}
		
	}

	
	/**
	 * Get all sections for supplied market
	 * @param market
	 * @throws ActionException
	 */
	protected void addSections(MarketVO market) throws ActionException {
		StringBuilder sql = new StringBuilder(350);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_MARKET m ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_MARKET_SECTION ms ");
		sql.append("ON m.MARKET_ID = ms.MARKET_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_SECTION s ");
		sql.append("ON s.SECTION_ID = ms.SECTION_ID ");
		sql.append("WHERE m.MARKET_ID = ? ");
		log.debug(sql);
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
	 * Get all markets from the database
	 * @param req
	 * @throws ActionException
	 */
	protected void retrieveMarkets(ActionRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(275);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT m.*, a.ATTRIBUTE_ID FROM ").append(customDb).append("BIOMEDGPS_MARKET m ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE_XR a ");
		sql.append("ON m.MARKET_ID = a.MARKET_ID ");
		sql.append("ORDER BY m.MARKET_ID ");
		log.debug(sql);
		Map<String, List<MarketVO>> markets = new TreeMap<>();
		// Add the default group here.
		markets.put(DEFAULT_GROUP, new ArrayList<MarketVO>());
		Tree attributeTree = buildAttributeTree();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			
			ResultSet rs = ps.executeQuery();
			String currentMarket = "";
			MarketVO market = null;
			DBProcessor db = new DBProcessor(dbConn);
			while(rs.next()) {
				if (!currentMarket.equals(rs.getString("MARKET_ID"))) {
					if (market != null) {
						addMarket(markets, attributeTree, market);
					}
					market = new MarketVO();
					db.executePopulate(market, rs);
					market.setUpdateDt(rs.getDate("UPDATE_DT"));
					buildUpdateMsg(market);
					currentMarket = rs.getString("MARKET_ID");
				}
				MarketAttributeVO attr = new MarketAttributeVO();
				db.executePopulate(attr, rs);
				market.addMarketAttribute(attr);
			}
			if (market == null) {
				addMarket(markets, attributeTree, market);
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
		super.putModuleData(markets);
	}
	
	
	/**
	 * Group the markets by thier attribute groups
	 * @param markets
	 * @param attributeTree
	 * @param market
	 */
	private void addMarket(Map<String, List<MarketVO>> markets, Tree attributeTree, MarketVO market) {
		// Markets use attributes from one branch of the tree
		// and are sorted accordingly. Markets without those attributes are
		// placed into the extras group.
		if (market.getMarketAttributes().isEmpty()) {
			markets.get(DEFAULT_GROUP).add(market);
			return;
		}
		
		String attrId = market.getMarketAttributes().get(0).getAttributeId();
		String[] path = attributeTree.findNode(attrId).getFullPath().split("/");
		
		// Markets using attributes too high up in the tree do not have enough
		// information to be sorted properly and are placed in the extras group.
		if (path.length < 2) {
			markets.get(DEFAULT_GROUP).add(market);
			return;
		}
		
		Node n = attributeTree.findNode(path[1]);
		
		if (!markets.keySet().contains(n.getNodeName())) {
			markets.put(n.getNodeName(), new ArrayList<MarketVO>());
		}
		
		markets.get(n.getNodeName()).add(market);
	}

	
	/**
	 * Create the full attribute tree in order to determine the full ancestry of each attribute
	 * @return
	 * @throws ActionException
	 */
	private Tree buildAttributeTree() throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_MARKET_ATTRIBUTE ");
		log.debug(sql);
		List<Node> attributes = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				Node n = new Node(rs.getString("ATTRIBUTE_ID"), rs.getString("PARENT_ID"));
				n.setNodeName(rs.getString("ATTRIBUTE_NM"));
				attributes.add(n);
			}
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		Tree t = new Tree(attributes);
		t.buildNodePaths();
		return t;
	}

}