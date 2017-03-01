package com.biomed.smarttrak.util;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;

import com.biomed.smarttrak.vo.MarketVO;
import com.biomed.smarttrak.vo.SectionVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.search.SearchDocumentHandler;
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

public class BiomedMarketIndexer  extends SMTAbstractIndex {
	private static final String ORG_ID = "BMG_SMARTTRAK";

	public static final String INDEX_TYPE = "BIOMEDGPS_MARKET";

	public BiomedMarketIndexer(Properties config) {
		this.config = config;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	@SuppressWarnings("resource")
	@Override
	public void addIndexItems(SolrClient server) {
		SolrActionUtil solrUtil = new SolrActionUtil(server);
		List<MarketVO> markets = retreiveMarkets(null);
		
		// Loop over each form transaction and turn it into a SolrStoryVO for processing
		for (MarketVO vo : markets) {
			try {
				solrUtil.addDocument(vo);
			} catch (Exception e) {
				log.error("could add to Solr", e);
			}
		}
	}
	
	/**
	 * Get all markets from the database
	 * @param id
	 * @return
	 */
	private List<MarketVO> retreiveMarkets(String id) {
		List<MarketVO> markets = new ArrayList<>();
		SmarttrakTree hierarchies = createHierarchies();
		String sql = buildRetrieveSql(id);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			if (id != null) ps.setString(1, id);
			
			ResultSet rs = ps.executeQuery();
			String currentMarket = "";
			MarketVO market = null;
			DBProcessor db = new DBProcessor(dbConn);
			while (rs.next()) {
				if (!currentMarket.equals(rs.getString("MARKET_ID"))) {
					if (market != null) markets.add(market);
					market = new MarketVO();
					db.executePopulate(market, rs);
					if (rs.getTimestamp("UPDATE_DT") != null) {
						market.setUpdateDt(rs.getDate("UPDATE_DT"));
					} else {
						market.setUpdateDt(rs.getDate("CREATE_DT"));
					}
					market.addOrganization(ORG_ID);
					market.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
					currentMarket = rs.getString("MARKET_ID");
				}
				if (!StringUtil.isEmpty(rs.getString("SECTION_ID"))) {
					addSection(market, hierarchies.findNode(rs.getString("SECTION_ID")));
				}
				
			}
			if (market != null) markets.add(market);
		} catch (SQLException e) {
			log.error(e);
		}
		
		
		return markets;
	}
	

	/**
	 * Add section id, name, and acl to document
	 */
	@SuppressWarnings("unchecked")
	protected void addSection(MarketVO market, Node n) throws SQLException {
		SectionVO sec = (SectionVO)n.getUserObject();
		market.addHierarchies(n.getFullPath());
		market.addSection(sec.getSectionNm());
		market.addACLGroup(Permission.GRANT, sec.getSolrTokenTxt());
		if (!market.getAttributes().containsKey("sectionId")) {
			market.addAttribute("sectionId", new ArrayList<String>());
		}
		((List<String>)market.getAttribute("sectionId")).add(sec.getSectionId());
	}

	
	/**
	 * Build the retrieve sql
	 * @param id
	 * @return
	 */
	private String buildRetrieveSql(String id) {
		StringBuilder sql = new StringBuilder(275);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT m.*, ms.SECTION_ID, s.SOLR_TOKEN_TXT FROM ").append(customDb).append("BIOMEDGPS_MARKET m ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_MARKET_SECTION ms ");
		sql.append("ON ms.MARKET_ID = m.MARKET_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_SECTION s ");
		sql.append("ON ms.SECTION_ID = s.SECTION_ID ");
		if (id != null) sql.append("WHERE m.MARKET_ID = ? ");
		return sql.toString();
	}
	
	
	/**
	 * Get a full hierarchy list
	 * @return
	 */
	private SmarttrakTree createHierarchies() {
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
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#purgeIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	@Override
	public void purgeIndexItems(SolrClient server) throws IOException {
		try {
			server.deleteByQuery(SearchDocumentHandler.INDEX_TYPE + ":" + getIndexType());
		} catch (Exception e) {
			throw new IOException(e);
		}
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTAbstractIndex#getIndexType()
	 */
	@Override
	public String getIndexType() {
		return INDEX_TYPE;
	}
	
	
	@Override
	public void addSingleItem(String id) {
		List<MarketVO> market = retreiveMarkets(id);
		try (SolrActionUtil util = new SolrActionUtil(super.makeServer())) {
			for (MarketVO vo : market) {
				util.addDocument(vo);
			}
		} catch (Exception e) {
			log.error("Failed to update market with id: " + id, e);
		}
	}
}