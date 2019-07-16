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
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;

//WC Custom
import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.admin.SectionHierarchyAction;
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
			util.addDocuments(retrieveMarkets());
		} catch (Exception e) {
			log.error("Failed to index markets", e);
		}
	}


	/**
	 * Purges multiple markets from Solr based on status
	 * @param marketIds
	 * @param statuses
	 */
	public void purgeIndexItems(String[] marketIds, String[] statuses){
		if(marketIds == null || statuses == null) return; //quick fail

		for (int i = 0; i < marketIds.length; i++) {
			String marketId = marketIds[i];
			String status = statuses[i];

			//if status is archived or deleted remove it
			if ("A".equals(status) || "D".equals(status)) {
				try {
					purgeSingleItem(marketId);
				} catch (IOException e) {
					log.warn("could not delete market from solr " + marketId, e);
				}
			}			
		}
	}

	/**
	 * Get all markets from the database
	 * @param id
	 * @return
	 */
	protected List<MarketVO> retrieveMarkets(String ...ids) {
		List<MarketVO> markets = new ArrayList<>();
		SmarttrakTree hierarchies = createHierarchies();
		String sql = buildRetrieveSql(ids);

		String currentMarketId = "";
		MarketVO market = null;
		DBProcessor db = new DBProcessor(dbConn);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			populateStatementMarks(ps, ids);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (!currentMarketId.equals(rs.getString("MARKET_ID"))) {
					if (market != null) markets.add(market);
					market = makeNewMarket(rs, db);
					currentMarketId = market.getMarketId();
				}

				String sectionId = rs.getString("SECTION_ID");
				if (!StringUtil.isEmpty(sectionId)) {
					Node n = hierarchies.findNode(sectionId);
					addSection(market, n);
					buildOrderString(hierarchies, n, market);
				}
			}
			//add that final market to the list
			if (market != null) 
				markets.add(market);

			buildContent(markets, ids);

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

		//Append the market's order number for intra-section ordering(overwrites region ordering)
		order.append(StringUtil.padLeft(StringUtil.checkVal(market.getOrderNo()), '0', 3));

		//Add the market's region code for intra-region ordering
		if(!StringUtil.isEmpty(market.getRegionCode())) {
			try{
				MarketVO.RegionOrder region = MarketVO.RegionOrder.valueOf(market.getRegionCode());
				order.append(StringUtil.padLeft(StringUtil.checkVal(region.getOrderVal()), '0', 3));
			}catch(IllegalArgumentException e) { 
				log.error("Region code not found within enum: " + e);
			} 	
		}

		market.addAttribute("order", order.toString());
	}


	/**
	 * Get all html attributes that constitute content for a market and combine
	 * them into a single contents field.
	 * @param markets
	 * @param id
	 * @throws SQLException
	 */
	protected void buildContent(List<MarketVO> markets, String... ids) throws SQLException {
		StringBuilder sql = new StringBuilder(275);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT x.MARKET_ID, x.VALUE_TXT FROM ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE_XR x ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE a ");
		sql.append("on a.ATTRIBUTE_ID = x.ATTRIBUTE_ID ");
		sql.append("WHERE a.TYPE_CD = 'HTML' ");
		addStatementMarks(sql, "x.MARKET_ID", ids);
		sql.append("ORDER BY x.MARKET_ID ");

		StringBuilder content = new StringBuilder(500);
		String currentMarket = "";
		Map<String, StringBuilder> contentMap = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			populateStatementMarks(ps, ids);

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
		vo.addAttribute("indent", rs.getInt("indent_no"));

		if (1 == rs.getInt("PUBLIC_FLG")) {
			vo.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
		} else if ("EU".equals(vo.getRegionCode())) { //EU region markets get a lesser permission level, correlates to a special User Role.
			vo.addRole(AdminControllerAction.EUREPORT_ROLE_LEVEL);
		} else if (!"P".equals(vo.getStatusNo())) { //preview mode, set role for staff or higher only
			vo.addRole(AdminControllerAction.STAFF_ROLE_LEVEL);
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
		log.debug("added hierarchy: " + n.getFullPath() + " tok=" + sec.getSolrTokenTxt());
		market.addHierarchies(n.getFullPath());
		market.addACLGroup(Permission.GRANT, sec.getSolrTokenTxt());
	}


	/**
	 * Build the retrieve sql
	 * @param id
	 * @return
	 */
	protected String buildRetrieveSql(String ...ids) {
		StringBuilder sql = new StringBuilder(275);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT m.market_id, m.market_nm, m.short_nm, m.indent_no, m.public_flg, m.status_no, m.region_cd, ms.SECTION_ID, s.SOLR_TOKEN_TXT, ");
		sql.append("greatest(coalesce(m.update_dt, m.create_dt), max(coalesce(xr.update_dt, xr.create_dt))) as mod_dt ");
		sql.append("FROM ").append(customDb).append("BIOMEDGPS_MARKET m ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE_XR xr ON m.MARKET_ID = xr.MARKET_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("BIOMEDGPS_MARKET_SECTION ms ON ms.MARKET_ID = m.MARKET_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("BIOMEDGPS_SECTION s ON ms.SECTION_ID = s.SECTION_ID ");
		sql.append("WHERE 1=1 ");
		addStatementMarks(sql, "m.MARKET_ID", ids);
		sql.append("group by  m.market_id, m.market_nm, m.short_nm, m.public_flg, m.indent_no, m.status_no, m.region_cd, m.create_dt, m.update_dt, ms.SECTION_ID, s.SOLR_TOKEN_TXT ");
		return sql.toString();
	}


	/**
	 * Get a full hierarchy list
	 * @return
	 */
	protected SmarttrakTree createHierarchies() {
		SectionHierarchyAction sha = new SectionHierarchyAction();
		sha.setAttributes(getAttributes());
		sha.setDBConnection(new SMTDBConnection(dbConn));
		//building our own Tree here preserves the root node (as a parent of MASTER_ROOT) 
		SmarttrakTree t = new SmarttrakTree(sha.getHierarchy());
		t.buildNodePaths();
		return t;
	}

	/**
	 * Helper method to add statement marks(?) into StringBuilder sql query
	 * @param sql
	 * @param ids
	 */
	private void addStatementMarks(StringBuilder sql, String primaryId, String... ids){
		if (ids != null && ids.length > 0) {
			sql.append("and ").append(primaryId).append(" in( ");
			DBUtil.preparedStatmentQuestion(ids.length, sql);
			sql.append(" ) ");
		}
	}

	/**
	 * Helper method to set the values into the PreparedStatement for us
	 * @param ps
	 * @param ids
	 * @throws SQLException
	 */
	private void populateStatementMarks(PreparedStatement ps, String... ids) throws SQLException{
		if (ids != null && ids.length > 0){
			int counter = 0;
			for (String id : ids) {
				ps.setString(++counter, id);
			}
		}		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTAbstractIndex#getIndexType()
	 */
	@Override
	public String getIndexType() {
		return INDEX_TYPE;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#indexItems(java.lang.String[])
	 */
	@Override
	public void indexItems(String... itemIds) {
		SolrClient server = makeServer();
		try (SolrActionUtil util = new SmarttrakSolrUtil(server)) {
			util.addDocuments(retrieveMarkets(itemIds));

		} catch (Exception e) {
			log.error("Failed to index markets", e);
		}
	}
}