package com.biomed.smarttrak.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.biomed.smarttrak.vo.CompanyVO;
import com.biomed.smarttrak.vo.LocationVO;
import com.biomed.smarttrak.vo.SectionVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
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

	private static final String COMPANY_ID  = "COMPANY_ID";
	private static final String SECTION_ID = "sectionId";

	public BiomedCompanyIndexer(Properties config) {
		this.config = config;
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
			util.addDocuments(retreiveCompanies(null));
		} catch (Exception e) {
			log.error("Failed to index companies", e);
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
			util.addDocuments(retreiveCompanies(id));
			server.commit(false, false); //commit, but don't wait for Solr to acknowledge
		} catch (Exception e) {
			log.error("Failed to index company with id: " + id, e);
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
				if (!currentCompany.equals(rs.getString(COMPANY_ID))) {
					addCompany(companies, company);
					company = buildSolrDocument(rs);
					currentCompany = rs.getString(COMPANY_ID);
				}
				if (!StringUtil.isEmpty(rs.getString("SECTION_ID")) && company != null) {
					addSection(company, hierarchies.findNode(rs.getString("SECTION_ID")));
				}

			}
			addCompany(companies, company);

			buildContent(companies, id);
			buildLocationInformation(companies);
			addProductAcls(companies, id, hierarchies);
		} catch (SQLException e) {
			log.error(e);
		}

		return companies;
	}


	/**
	 * Get the acl permissions for all products associated with 
	 * this company, either through ownership or alliance.
	 * @param companies
	 * @param id
	 * @param hierarchies
	 */
	@SuppressWarnings("unchecked")
	private void addProductAcls(List<SecureSolrDocumentVO> companies, String id, SmarttrakTree hierarchies) {
		Map<String, List<SectionVO>> productAcls = loadProductAcls(id, hierarchies);
		
		for (SecureSolrDocumentVO company : companies) {
			List<SectionVO> aclList = productAcls.get(company.getDocumentId());
			if (aclList == null) continue;

			List<String> sectionIds = ((List<String>)company.getAttribute(SECTION_ID));
			// If the company didn't have any section ids create a new list for them.
			if (sectionIds == null) sectionIds = new ArrayList<>();
			for (SectionVO s : aclList) {
				company.addACLGroup(Permission.GRANT, s.getSolrTokenTxt());
				sectionIds.add(s.getSectionId());
			}
			// Add the section ids to the document. This will either be the newly appended
			// original list of a brand new one.
			company.addAttribute(SECTION_ID, sectionIds);
		}
	}

	
	/**
	 * Load all products and their alliances
	 * @param id
	 * @param hierarchies
	 * @return
	 */
	private Map<String, List<SectionVO>> loadProductAcls(String id, SmarttrakTree hierarchies) {
		String sql = getProductSql(id);
		Map<String, List<SectionVO>> acls = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			if (!StringUtil.isEmpty(id)) {
				ps.setString(1, id);
				ps.setString(2, id);
			}
			
			String currentProduct = "";
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				String sectionId = rs.getString("SECTION_ID");
				if (StringUtil.isEmpty(sectionId))continue;
				
				if (!currentProduct.equals(rs.getString("PRODUCT_ID"))) {
					currentProduct = rs.getString("PRODUCT_ID");
					// Add the acl for the owning company
					addAcl(sectionId, hierarchies, rs.getString("COMPANY_ID"), acls);
				}
				// Add the acl for the allied company
				addAcl(sectionId, hierarchies, rs.getString("ALLY_ID"), acls);
			}
			
		} catch (SQLException e) {
			
		}
		return acls;
	}
	
	
	/**
	 * Add the requested section to the list associated with the supplied company
	 * @param sectionId
	 * @param hierarchies
	 * @param companyId
	 * @param acls
	 */
	private void addAcl(String sectionId, SmarttrakTree hierarchies, String companyId, Map<String, List<SectionVO>> acls) {
		Node n = hierarchies.findNode(sectionId);
		if (n == null) return;
		SectionVO sec = (SectionVO)n.getUserObject();
		String docId = Section.COMPANY+"_"+companyId;
		if (!acls.keySet().contains(docId))
			acls.put(docId, new ArrayList<>());
		
		acls.get(docId).add(sec);
	}

	
	/**
	 * Build the product sql
	 * @param id
	 * @return
	 */
	private String getProductSql(String id) {
		StringBuilder sql = new StringBuilder(300);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT p.PRODUCT_ID, p.COMPANY_ID, ps.SECTION_ID, a.COMPANY_ID as ALLY_ID FROM ");
		sql.append(customDb).append("BIOMEDGPS_PRODUCT p ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT_SECTION ps ");
		sql.append("ON ps.PRODUCT_ID = p.PRODUCT_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("biomedgps_product_alliance_xr a ");
		sql.append("ON a.PRODUCT_ID = p.PRODUCT_ID ");
		sql.append("WHERE p.STATUS_NO not in ('A','D') ");
		if (id != null) sql.append("and p.COMPANY_ID = ? or a.COMPANY_ID = ? ");
		sql.append("ORDER BY COMPANY_ID ");
		log.info(sql);
		return sql.toString();
	}

	/**
	 * Check to see if the supplied company is viable and if so add it.
	 * @param companies
	 * @param company
	 */
	protected void addCompany(List<SecureSolrDocumentVO> companies,
			SecureSolrDocumentVO company) {
		if (company != null) 
			companies.add(company);
	}

	/**
	 * Get all html attributes that constitute content for a company and combine
	 * them into a single contents field.
	 * @param companies
	 * @param id
	 * @throws SQLException
	 */
	protected void buildContent(List<SecureSolrDocumentVO> companies, String id) throws SQLException {
		StringBuilder sql = new StringBuilder(275);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT x.COMPANY_ID, x.VALUE_TXT FROM ").append(customDb).append("BIOMEDGPS_COMPANY_ATTRIBUTE_XR x ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY_ATTRIBUTE a on a.ATTRIBUTE_ID = x.ATTRIBUTE_ID ");
		sql.append("WHERE a.TYPE_NM = 'HTML' ");
		if (!StringUtil.isEmpty(id)) sql.append("and x.COMPANY_ID = ? ");
		sql.append("ORDER BY x.COMPANY_ID ");

		StringBuilder content = new StringBuilder();
		String currentCompany = "";
		Map<String, StringBuilder> contentMap = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			if (!StringUtil.isEmpty(id)) ps.setString(1, id);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (!currentCompany.equals(rs.getString(COMPANY_ID))) {
					addContent(currentCompany, content, contentMap);
					content = new StringBuilder(1024);
					currentCompany = rs.getString(COMPANY_ID);
				}
				if (content.length() > 1) content.append("\n");
				content.append(rs.getString("VALUE_TXT"));
			}
			addContent(currentCompany, content, contentMap);
		}

		for (SecureSolrDocumentVO company : companies) {
			if (contentMap.get(company.getDocumentId()) == null) continue;
			company.setContents(contentMap.get(company.getDocumentId()).toString());
		}
	}


	/**
	 * Ensure content is viable and if so add it to the map
	 * @param currentMarket
	 * @param content
	 * @param contentMap
	 */
	protected void addContent(String currentCompany, StringBuilder content,
			Map<String, StringBuilder> contentMap) {
		if (content.length() > 0) {
			contentMap.put(Section.COMPANY+"_"+currentCompany, content);
		}
	}

	/**
	 * Add section id, name, and acl to document
	 */
	@SuppressWarnings("unchecked")
	protected void addSection(SecureSolrDocumentVO company, Node n) {
		SectionVO sec = (SectionVO)n.getUserObject();
		company.addHierarchies(n.getFullPath());
		company.addSection(sec.getSectionNm());
		company.addACLGroup(Permission.GRANT, sec.getSolrTokenTxt());
		if (!company.getAttributes().containsKey(SECTION_ID)) {
			company.addAttribute(SECTION_ID, new ArrayList<String>());
		}
		((List<String>)company.getAttribute(SECTION_ID)).add(sec.getSectionId());
	}

	/**
	 * Build a solr document from the supplied resltset
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private SecureSolrDocumentVO buildSolrDocument(ResultSet rs) throws SQLException {
		SecureSolrDocumentVO company = new SecureSolrDocumentVO(INDEX_TYPE);
		CompanyVO.setSolrId(company, rs.getString(COMPANY_ID));
		company.setTitle(rs.getString("COMPANY_NM"));
		SmarttrakSolrUtil.setSearchField(rs.getString("SHORT_NM_TXT"), "shortNm", company);
		company.addAttribute("status", rs.getString("STATUS_NO"));
		company.addAttribute("ticker", rs.getString("NAME_TXT"));
		company.setDocumentUrl(AdminControllerAction.Section.COMPANY.getPageURL()+config.getProperty(Constants.QS_PATH)+rs.getString(COMPANY_ID));
		company.addAttribute("productCount", rs.getInt("PRODUCT_NO"));
		SmarttrakSolrUtil.setSearchField(rs.getString("PARENT_NM"), "parentNm", company);

		if (rs.getTimestamp("UPDATE_DT") != null) {
			company.setUpdateDt(rs.getDate("UPDATE_DT"));
		} else {
			company.setUpdateDt(rs.getDate("CREATE_DT"));
		}
		company.addOrganization(ORG_ID);
		if (1 == rs.getInt("PUBLIC_FLG")) {
			company.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
		} else if ("E".equals(rs.getString("STATUS_NO"))) {
			company.addRole(AdminControllerAction.STAFF_ROLE_LEVEL);
		} else {
			company.addRole(AdminControllerAction.DEFAULT_ROLE_LEVEL); //any logged in ST user can see this.
		}

		return company;
	}


	/**
	 * Build the retrieve sql
	 * @param id
	 * @return
	 */
	private String buildRetrieveSql(String id) {
		StringBuilder sql = new StringBuilder(1250);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT c.COMPANY_ID, a.SECTION_ID, c.COMPANY_NM, c.STATUS_NO, e.NAME_TXT, c.PUBLIC_FLG, c.SHORT_NM_TXT, ");
		sql.append("c2.COMPANY_NM as PARENT_NM, COUNT(p.COMPANY_ID) as PRODUCT_NO, c.CREATE_DT, c.UPDATE_DT ");
		sql.append("FROM ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT p ");
		sql.append("ON p.COMPANY_ID = c.COMPANY_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_STOCK_EXCHANGE e ");
		sql.append("ON e.EXCHANGE_ID = c.EXCHANGE_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY_ATTRIBUTE_XR xr ");
		sql.append("ON xr.COMPANY_ID = c.COMPANY_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY_ATTRIBUTE a ");
		sql.append("ON a.ATTRIBUTE_ID = xr.ATTRIBUTE_ID and a.SECTION_ID is not null ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY_SECTION cs ");
		sql.append("ON cs.COMPANY_ID = c.COMPANY_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY c2 ");
		sql.append("ON c2.COMPANY_ID = c.PARENT_ID ");
		sql.append("WHERE c.STATUS_NO not in ('A','D') and p.STATUS_NO not in ('A', 'D', 'E') ");
		if (id != null) sql.append("and c.COMPANY_ID = ? ");
		sql.append("GROUP BY c.COMPANY_ID, c.COMPANY_NM, a.SECTION_ID, c.STATUS_NO, ");
		sql.append("e.NAME_TXT, p.COMPANY_ID, c2.COMPANY_NM, c.CREATE_DT, c.UPDATE_DT ");
		sql.append("having COUNT(p.COMPANY_ID) > 0 ");
		sql.append("order by c.company_id ");
		log.debug(sql);
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
	
	/**
	 * Get the state and country for the company that owns each product
	 * and assign that information to the solr document.
	 * @param companies
	 */
	protected void buildLocationInformation(List<SecureSolrDocumentVO> companies) {
		Map<String, LocationVO> locationMap = retrieveLocations();
		for (SecureSolrDocumentVO company : companies) {
			String companyId = company.getDocumentId();
			LocationVO loc = locationMap.get(companyId);
			if (loc == null) continue;
			company.setState(loc.getStateCode());
			company.setCountry(loc.getCountryName());
		}
	}
	
	/**
	 * Get a collection of all companies and thier primary locations
	 * @return
	 */
	protected Map<String, LocationVO> retrieveLocations() {
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(150);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_COMPANY_LOCATION l ");
		sql.append("LEFT JOIN COUNTRY c on c.COUNTRY_CD = l.COUNTRY_CD ");
		sql.append("ORDER BY COMPANY_ID, PRIMARY_LOCN_FLG DESC ");

		DBProcessor db = new DBProcessor(dbConn);
		List<Object> results = db.executeSelect(sql.toString(), null, new LocationVO());
		Map<String, LocationVO> locations = new HashMap<>();
		for (Object o : results) {
			LocationVO vo = (LocationVO) o;
			// The first location for each company is it's primary location, others can be ignored.
			if (!locations.containsKey(vo.getCompanyId()))
				locations.put(Section.COMPANY + "_" + vo.getCompanyId(), vo);
		}

		return locations;
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTAbstractIndex#getIndexType()
	 */
	@Override
	public String getIndexType() {
		return INDEX_TYPE;
	}
}