package com.biomed.smarttrak.util;

//Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;



//Solr 5.5
import org.apache.solr.client.solrj.SolrClient;



// SMT base libs
import com.siliconmtn.data.Node;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;

//WC Custom
import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.vo.MarketVO;
import com.biomed.smarttrak.vo.SectionVO;

//WC
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SecureSolrDocumentVO.Permission;

/****************************************************************************
 * <b>Title</b>: BiomedMarketIndexer.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Index all markets.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Feb 15, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class MarketIndexer  extends SMTAbstractIndex {

	public static final String INDEX_TYPE = "BIOMEDGPS_MARKET";
	private final String baseUrl;

	public MarketIndexer(Properties config) {
		this.config = config;
		baseUrl = AdminControllerAction.Section.MARKET.getPageURL() + config.getProperty(Constants.QS_PATH);
	}

	public static MarketIndexer makeInstance(Map<String, Object> attributes) {
		return new MarketIndexer(makeProperties(attributes));
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	@SuppressWarnings("resource")
	@Override
	public void addIndexItems(SolrClient server) {
		// Never place this in a try with resources.
		// This server was given to this method and it is not this method's
		// job or right to close it.
		SolrActionUtil util = new SmarttrakSolrUtil(server);
		try {
			util.addDocuments(retrieveMarkets(null));
		} catch (Exception e) {
			log.error("Failed to index markets", e);
		}
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addSingleItem(java.lang.String)
	 */
	@Override
	public void addSingleItem(String id) {
		SolrClient server = makeServer();
		try (SolrActionUtil util = new SmarttrakSolrUtil(server)) {
			util.addDocuments(retrieveMarkets(id));
			server.commit(false, false); //commit, but don't wait for Solr to acknowledge
		} catch (Exception e) {
			log.error("Failed to index market with id: " + id, e);
		}
	}


	/**
	 * Get all markets from the database
	 * @param id
	 * @return
	 */
	protected List<MarketVO> retrieveMarkets(String id) {
		List<MarketVO> markets = new ArrayList<>();
		SmarttrakTree hierarchies = createHierarchies();
		String sql = buildRetrieveSql(id);

		String currentMarketId = "";
		MarketVO market = null;
		DBProcessor db = new DBProcessor(dbConn);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			if (id != null) ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (!currentMarketId.equals(rs.getString("MARKET_ID"))) {
					if (market != null) markets.add(market);
					market = makeNewMarket(rs, db);
					currentMarketId = market.getMarketId();
				}
				if (!StringUtil.isEmpty(rs.getString("SECTION_ID"))) {
					Node n = hierarchies.findNode(rs.getString("SECTION_ID"));
					addSection(market, n);
					buildOrderString(hierarchies, n, market);
				}
			}
			//add that final market to the list
			if (market != null) 
				markets.add(market);

			buildContent(markets, id);

		} catch (SQLException e) {
			log.error("could not load Market for Solr", e);
		}

		return markets;
	}


	/**
	 * Combine the order number of the market section level and all ancestors
	 * into a single string that can be used in the sort in order to mirror
	 * the order used in the rest of the site.
	 * @param hierarchies
	 * @param n
	 * @param market
	 */
	protected void buildOrderString(SmarttrakTree hierarchies, Node n, MarketVO market) {
		StringBuilder order = new StringBuilder(1000);

		// Cast the order number to a string and pad it out 
		// so that all order numbers are the same size
		String hierarchyOrder = StringUtil.checkVal(((SectionVO)n.getUserObject()).getOrderNo());
		hierarchyOrder = StringUtil.padLeft(hierarchyOrder, '0', 3);
		order.append(hierarchyOrder);

		// Traverse the tree until you reach the top
		while(!StringUtil.isEmpty(n.getParentId())) {
			n = hierarchies.findNode(n.getParentId());
			hierarchyOrder = StringUtil.checkVal(((SectionVO)n.getUserObject()).getOrderNo());
			hierarchyOrder = StringUtil.padLeft(hierarchyOrder, '0', 3);

			// Insert the padded out order number at the start of the string
			// so as to match the path back up the tree.
			order.insert(0, hierarchyOrder);
		}
		
		// End with appending the market's order number for intra-section ordering
		order.append(StringUtil.padLeft(StringUtil.checkVal(market.getOrderNo()), '0', 3));
		
		market.addAttribute("order", order.toString());
	}


	/**
	 * Get all html attributes that constitute content for a market and combine
	 * them into a single contents field.
	 * @param markets
	 * @param id
	 * @throws SQLException
	 */
	protected void buildContent(List<MarketVO> markets, String id) throws SQLException {
		StringBuilder sql = new StringBuilder(275);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT x.MARKET_ID, x.VALUE_TXT FROM ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE_XR x ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE a ");
		sql.append("on a.ATTRIBUTE_ID = x.ATTRIBUTE_ID ");
		sql.append("WHERE a.TYPE_CD = 'HTML' ");
		if (!StringUtil.isEmpty(id)) sql.append("and x.MARKET_ID = ? ");
		sql.append("ORDER BY x.MARKET_ID ");

		StringBuilder content = new StringBuilder(500);
		String currentMarket = "";
		Map<String, StringBuilder> contentMap = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			if (!StringUtil.isEmpty(id)) ps.setString(1, id);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if(!currentMarket.equals(rs.getString("MARKET_ID"))) {
					if (content.length() > 0) {
						contentMap.put(currentMarket, content);
					}
					content = new StringBuilder(1024);
					currentMarket = rs.getString("MARKET_ID");
				}
				if (content.length() > 1) content.append("\n");
				content.append(rs.getString("VALUE_TXT"));
			}
			if (content.length() > 0) {
				contentMap.put(currentMarket, content);
			}
		}

		for (MarketVO market : markets) {
			if (contentMap.get(market.getMarketId()) == null) continue;
			market.setContents(contentMap.get(market.getMarketId()).toString());
		}
	}


	/**
	 * creates the initial MarketVO using the ResultSet and some Smarttrak constants
	 * @param rs
	 * @param db
	 * @return
	 * @throws SQLException
	 */
	protected MarketVO makeNewMarket(ResultSet rs, DBProcessor db) throws SQLException {
		MarketVO vo = new MarketVO();
		db.executePopulate(vo, rs);
		vo.setUpdateDt(rs.getTimestamp("mod_dt"));
		vo.addOrganization(AdminControllerAction.BIOMED_ORG_ID);

		if (1 == rs.getInt("PUBLIC_FLG")) {
			vo.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
		} else if ("E".equals(vo.getStatusNo())) { //preview mode, set role for staff or higher only
			vo.addRole(AdminControllerAction.STAFF_ROLE_LEVEL);
		} else if ("EU".equals(vo.getRegionCode())) { //EU region markets get a lesser permission level, correlates to a special User Role.
			vo.addRole(AdminControllerAction.EUREPORT_ROLE_LEVEL);
		} else {
			vo.addRole(AdminControllerAction.DEFAULT_ROLE_LEVEL); //any logged in ST user can see this.
		}
		vo.setDocumentUrl(baseUrl+vo.getMarketId());
		return vo;
	}


	/**
	 * Add section id, name, and acl to document
	 */
	protected void addSection(MarketVO market, Node n) {
		SectionVO sec = (SectionVO)n.getUserObject();
		market.addHierarchies(n.getFullPath());
		market.addACLGroup(Permission.GRANT, sec.getSolrTokenTxt());
	}


	/**
	 * Build the retrieve sql
	 * @param id
	 * @return
	 */
	protected String buildRetrieveSql(String id) {
		StringBuilder sql = new StringBuilder(275);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT m.*, coalesce(m.update_dt, m.create_dt) as mod_dt, ms.SECTION_ID, s.SOLR_TOKEN_TXT ");
		sql.append("FROM ").append(customDb).append("BIOMEDGPS_MARKET m ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_MARKET_SECTION ms ON ms.MARKET_ID = m.MARKET_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_SECTION s ON ms.SECTION_ID = s.SECTION_ID ");
		sql.append("WHERE m.status_no not in ('A','D') "); //do not push archived or deleted markets to Solr
		if (id != null) sql.append("and m.MARKET_ID = ? ");
		return sql.toString();
	}


	/**
	 * Get a full hierarchy list
	 * @return
	 */
	protected SmarttrakTree createHierarchies() {
		//TODO replace all of this code with a call to the SectionAction to load the default tree.
		StringBuilder sql = new StringBuilder(125);
		sql.append("SELECT * FROM ").append(config.getProperty(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_SECTION ");
		log.info(sql);
		List<Node> markets = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				SectionVO sec = new SectionVO(rs);
				Node n = new Node(sec.getSectionId(), sec.getParentId());
				n.setNodeName(sec.getSectionNm());
				n.setUserObject(sec);
				markets.add(n);
			}
		} catch (SQLException e) {
			log.error(e);
		}

		SmarttrakTree t = new SmarttrakTree(markets);
		t.buildNodePaths();

		return t;
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTAbstractIndex#getIndexType()
	 */
	@Override
	public String getIndexType() {
		return INDEX_TYPE;
	}
}