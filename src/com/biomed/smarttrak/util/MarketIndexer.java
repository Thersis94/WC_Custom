package com.biomed.smarttrak.util;

//Java 8
import java.io.IOException;
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
	private String baseUrl;

	public MarketIndexer(Properties config) {
		this.config = config;
		baseUrl = AdminControllerAction.Section.MARKET.getPageURL() + config.getProperty(Constants.QS_PATH);
	}

	public static MarketIndexer makeInstance(Map<String, Object> attributes) {
		Properties props = new Properties();
		props.putAll(attributes);
		return new MarketIndexer(props);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	@Override
	public void addIndexItems(SolrClient server) {
		pushMarkets(server, null);
	}

	@Override
	public void addSingleItem(String id) {
		try (SolrClient server = makeServer()) {
			pushMarkets(server, id);
		} catch (IOException e) {
			log.error("Failed to add document with id: " + id, e);
		}
	}


	/**
	 * reused method for pushing all, or one, market over to Solr
	 * @param server
	 * @param pkId
	 */
	@SuppressWarnings("resource")
	protected void pushMarkets(SolrClient server, String pkId) {
		SolrActionUtil util = new SmarttrakSolrUtil(server);
		try {
			util.addDocuments(retrieveMarkets(pkId));
		} catch (Exception e) {
			log.error("Failed to update market in Solr, passed pkid=" + pkId, e);
		}
	}


	/**
	 * Get all markets from the database
	 * @param id
	 * @return
	 */
	private List<MarketVO> retrieveMarkets(String id) {
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
				if (!StringUtil.isEmpty(rs.getString("SECTION_ID")))
					addSection(market, hierarchies.findNode(rs.getString("SECTION_ID")));
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
		
		Map<String, StringBuilder> contentMap = new HashMap<>();
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			if (!StringUtil.isEmpty(id)) ps.setString(1, id);
			
			ResultSet rs = ps.executeQuery();
			StringBuilder content = new StringBuilder();
			String currentMarket = "";
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

		if ("E".equals(vo.getStatusNo())) { //preview mode, set role for staff or higher only
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
	protected void addSection(MarketVO market, Node n) throws SQLException {
		SectionVO sec = (SectionVO)n.getUserObject();
		market.addHierarchies(n.getFullPath());
		market.addACLGroup(Permission.GRANT, sec.getSolrTokenTxt());
	}


	/**
	 * Build the retrieve sql
	 * @param id
	 * @return
	 */
	private String buildRetrieveSql(String id) {
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
	private SmarttrakTree createHierarchies() {
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