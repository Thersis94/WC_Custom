package com.biomed.smarttrak.util;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.vo.SectionVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SecureSolrDocumentVO;
import com.smt.sitebuilder.util.solr.SecureSolrDocumentVO.Permission;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

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
		List<SecureSolrDocumentVO> companies = retreiveCompanies(null);
		
		// Loop over each form transaction and turn it into a SolrStoryVO for processing
		for (SecureSolrDocumentVO vo : companies) {
			try {
				solrUtil.addDocument(vo);
			} catch (Exception e) {
				log.error("could add to Solr", e);
			}
		}
	}
	
	
	/**
	 * Get all companies
	 * @param id
	 * @return
	 */
	private List<SecureSolrDocumentVO> retreiveCompanies(String id) {
		List<SecureSolrDocumentVO> companies = new ArrayList<>();
		String sql = buildRetrieveSql(id);
		SmarttrakTree hierarchies = createHierarchies();
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			if (id != null) ps.setString(1, id);
			
			ResultSet rs = ps.executeQuery();
			String currentCompany = "";
			SecureSolrDocumentVO company = null;
			while (rs.next()) {
				if (!currentCompany.equals(rs.getString("COMPANY_ID"))) {
					if (company != null) companies.add(company);
					company = buildSolrDocument(rs);
					currentCompany = rs.getString("COMPANY_ID");
				}
				if (!StringUtil.isEmpty(rs.getString("SECTION_ID")) && company != null) {
					addSection(company, hierarchies.findNode(rs.getString("SECTION_ID")));
				}
				
			}
			if (company != null) companies.add(company);
		} catch (SQLException e) {
			log.error(e);
		}
		
		return companies;
	}

	/**
	 * Add section id, name, and acl to document
	 */
	@SuppressWarnings("unchecked")
	protected void addSection(SecureSolrDocumentVO company, Node n) throws SQLException {
		SectionVO sec = (SectionVO)n.getUserObject();
		company.addHierarchies(n.getFullPath());
		company.addSection(sec.getSectionNm());
		company.addACLGroup(Permission.GRANT, sec.getSolrTokenTxt());
		if (!company.getAttributes().containsKey("sectionId")) {
			company.addAttribute("sectionId", new ArrayList<String>());
		}
		((List<String>)company.getAttribute("sectionId")).add(sec.getSectionId());
	}

	/**
	 * Build a solr document from the supplied resltset
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private SecureSolrDocumentVO buildSolrDocument(ResultSet rs) throws SQLException {
		SecureSolrDocumentVO company = new SecureSolrDocumentVO(INDEX_TYPE);
		company.setDocumentId(rs.getString("COMPANY_ID"));
		company.setTitle(rs.getString("COMPANY_NM"));
		company.setContentType(rs.getString("STATUS_NO"));
		company.addAttribute("ticker", rs.getString("NAME_TXT"));
		company.setDocumentUrl(AdminControllerAction.Section.COMPANY.getPageURL()+config.getProperty(Constants.QS_PATH)+rs.getString("COMPANY_ID"));
		company.addAttribute("productCount", rs.getInt("PRODUCT_NO"));
		company.addAttribute("parentNm", rs.getString("PARENT_NM"));
		
		if (rs.getTimestamp("UPDATE_DT") != null) {
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
		StringBuilder sql = new StringBuilder(1000);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT c.COMPANY_ID, cs.SECTION_ID, c.COMPANY_NM, c.STATUS_NO, e.NAME_TXT, ");
		sql.append("c2.COMPANY_NM as PARENT_NM, COUNT(p.COMPANY_ID) as PRODUCT_NO, c.CREATE_DT, c.UPDATE_DT ");
		sql.append("FROM ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT p ");
		sql.append("ON p.COMPANY_ID = c.COMPANY_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_STOCK_EXCHANGE e ");
		sql.append("ON e.EXCHANGE_ID = c.EXCHANGE_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY_SECTION cs ");
		sql.append("ON cs.COMPANY_ID = c.COMPANY_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY c2 ");
		sql.append("ON c2.COMPANY_ID = c.PARENT_ID ");
		if (id != null) sql.append("WHERE c.COMPANY_ID = ? ");
		sql.append("GROUP BY c.COMPANY_ID, c.COMPANY_NM, cs.SECTION_ID, c.STATUS_NO, ");
		sql.append("e.NAME_TXT, p.COMPANY_ID, c2.COMPANY_NM, c.CREATE_DT, c.UPDATE_DT ");
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
		List<SecureSolrDocumentVO> company = retreiveCompanies(id);
		try (SolrActionUtil util = new SolrActionUtil(super.makeServer())) {
			for (SecureSolrDocumentVO vo : company) {
				util.addDocument(vo);
			}
		} catch (Exception e) {
			log.error("Failed to update company with id: " + id, e);
		}
	}
}