package com.biomed.smarttrak.util;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;

import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: BiomedCompanyIndexer.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Index all companies.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Feb 15, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class BiomedCompanyIndexer  extends SMTAbstractIndex {
	private static final String ORG_ID = "BMG_SMARTTRAK";
	public static final String INDEX_TYPE = "BIOMEDGPS_COMPANY";

	public BiomedCompanyIndexer(Properties config) {
		this.config = config;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	@SuppressWarnings("resource")
	@Override
	public void addIndexItems(SolrClient server) {
		SolrActionUtil solrUtil = new SolrActionUtil(server);
		List<SolrDocumentVO> companies = retreiveCompanies(null);
		
		// Loop over each form transaction and turn it into a SolrStoryVO for processing
		for (SolrDocumentVO vo : companies) {
			try {
				solrUtil.addDocument(vo);
			} catch (Exception e) {
				log.error("could add to Solr", e);
			}
		}
		solrUtil = null;
	}
	
	
	/**
	 * Get all companies
	 * @param id
	 * @return
	 */
	private List<SolrDocumentVO> retreiveCompanies(String id) {
		List<SolrDocumentVO> companies = new ArrayList<>();
		Map<String, String> hierarchies = createHierarchies();
		String sql = buildRetrieveSql(id);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			if (id != null) ps.setString(1, id);
			
			ResultSet rs = ps.executeQuery();
			String currentCompany = "";
			SolrDocumentVO company = null;
			while (rs.next()) {
				if (!currentCompany.equals(rs.getString("COMPANY_ID"))) {
					if (company != null) companies.add(company);
					company = buildSolrDocument(rs);
					currentCompany = rs.getString("COMPANY_ID");
				}
				if (rs.getString("SECTION_ID") != null) {
					company.addSection(hierarchies.get(rs.getString("SECTION_ID")));
				}
				
			}
			if (company != null) companies.add(company);
		} catch (SQLException e) {
			log.error(e);
		}
		
		return companies;
	}
	
	
	/**
	 * Build a solr document from the supplied resltset
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private SolrDocumentVO buildSolrDocument(ResultSet rs) throws SQLException {
		SolrDocumentVO company = new SolrDocumentVO(INDEX_TYPE);
		company.setDocumentId(rs.getString("COMPANY_ID"));
		company.setTitle(rs.getString("COMPANY_NM"));
		company.setContentType(rs.getString("STATUS_NO"));
		company.addAttribute("ticker", rs.getString("NAME_TXT"));
		
		if (!(rs.getTimestamp("UPDATE_DT") == null)) {
			company.setUpdateDt(rs.getDate("UPDATE_DT"));
		} else {
			company.setUpdateDt(rs.getDate("CREATE_DT"));
		}
		company.addOrganization(ORG_ID);
		company.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
		
		return company;
	}

	
	/**
	 * Build the retrieve sql
	 * @param id
	 * @return
	 */
	private String buildRetrieveSql(String id) {
		StringBuilder sql = new StringBuilder(275);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT c.*, cs.SECTION_ID, e.NAME_TXT FROM ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY_SECTION cs ");
		sql.append("ON cs.COMPANY_ID = c.COMPANY_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_STOCK_EXCHANGE e ");
		sql.append("ON e.EXCHANGE_ID = c.EXCHANGE_ID ");
		if (id != null) sql.append("WHERE c.COMPANY_ID = ? ");
		return sql.toString();
	}
	
	
	/**
	 * Create a full hierarchy list
	 * @return
	 */
	private Map<String, String> createHierarchies() {
		Map<String, String> hierarchies = new HashMap<>();
		StringBuilder sql = new StringBuilder(125);
		sql.append("SELECT * FROM ").append(config.getProperty(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_SECTION ");
		log.info(sql);
		List<Node> companies = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				Node n = new Node(rs.getString("SECTION_ID"), rs.getString("PARENT_ID"));
				n.setNodeName(rs.getString("SECTION_NM"));
				companies.add(n);
			}
		} catch (SQLException e) {
			log.error(e);
		}
		
		Tree t = new Tree(companies);
		t.buildNodePaths("~");
		
		for (Node n : t.preorderList()) {
			hierarchies.put(n.getNodeId(), n.getFullPath());
		}
		
		return hierarchies;
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
		List<SolrDocumentVO> company = retreiveCompanies(id);
		try (SolrActionUtil util = new SolrActionUtil(super.makeServer())) {
			for (SolrDocumentVO vo : company) {
				util.addDocument(vo);
			}
		} catch (Exception e) {
			log.error("Failed to update company with id: " + id, e);
		}
	}
}