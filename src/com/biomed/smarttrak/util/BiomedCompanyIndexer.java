package com.biomed.smarttrak.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.biomed.smarttrak.admin.SectionHierarchyAction;
import com.biomed.smarttrak.vo.CompanyVO;
import com.biomed.smarttrak.vo.LocationVO;
import com.biomed.smarttrak.vo.ProductAllianceVO;
import com.biomed.smarttrak.vo.ProductVO;
import com.biomed.smarttrak.vo.SectionVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
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
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#indexItems(java.lang.String[])
	 */
	@Override
	public void indexItems(String... itemIds) {
		SolrClient server = makeServer();
		try (SolrActionUtil util = new SmarttrakSolrUtil(server)) {
			for (String id : itemIds)
				util.addDocuments(retreiveCompanies(id));

		} catch (Exception e) {
			log.error("Failed to index company with id: " + itemIds, e);
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
			addProducts(companies, id, hierarchies);
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
	private void addProducts(List<SecureSolrDocumentVO> companies, String id, SmarttrakTree hierarchies) {
		Map<String, List<ProductVO>> products = loadProductAcls(id, hierarchies);
		for (SecureSolrDocumentVO company : companies) {
			List<ProductVO> productList = products.get(company.getDocumentId());
			if (productList == null) continue;

			List<String> sectionIds = ((List<String>)company.getAttribute(SECTION_ID));
			// If the company didn't have any section ids create a new list for them.
			if (sectionIds == null) sectionIds = new ArrayList<>();
			addProductInformation(productList, company, sectionIds);
		}
	}


	/**
	 * Add all product names, short names, aliases, and acls to the company
	 * @param productList
	 * @param company
	 * @param sectionIds
	 */
	private void addProductInformation(List<ProductVO> productList, SecureSolrDocumentVO company, List<String> sectionIds) {
		Set<String> productName = new HashSet<>(productList.size());
		Set<String> productShortName = new HashSet<>(productList.size());
		Set<String> productAliasName = new HashSet<>(productList.size());
		for (ProductVO p : productList) {
			for (SectionVO s : p.getProductSections()) {
				company.addACLGroup(Permission.GRANT, s.getSolrTokenTxt());
				sectionIds.add(s.getSectionId());
			}
			if (!StringUtil.isEmpty(p.getProductName())) productName.add(p.getProductName().toLowerCase());
			if (!StringUtil.isEmpty(p.getShortName())) productShortName.add(p.getShortName().toLowerCase());
			if (!StringUtil.isEmpty(p.getAliasName())) productAliasName.add(p.getAliasName().toLowerCase());
		}

		company.addAttribute("productNames", productName);
		company.addAttribute("productShortNames", productShortName);
		company.addAttribute("productAliasNames", productAliasName);
		// Add the section ids to the document. This will either be the newly appended
		// original list of a brand new one.
		company.addAttribute(SECTION_ID, sectionIds);
	}

	/**
	 * Load all products and their alliances
	 * @param id
	 * @param hierarchies
	 * @return
	 */
	private Map<String, List<ProductVO>> loadProductAcls(String id, SmarttrakTree hierarchies) {
		String sql = getProductSql(id);
		Map<String, List<ProductVO>> acls = new HashMap<>();
		DBProcessor db = new DBProcessor(dbConn);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			if (!StringUtil.isEmpty(id)) {
				ps.setString(1, id);
				ps.setString(2, id);
			}

			String currentProduct = "";
			ResultSet rs = ps.executeQuery();
			ProductVO p = new ProductVO();
			while(rs.next()) {

				if (!currentProduct.equals(rs.getString("PRODUCT_ID"))) {
					addAllProducts(p, acls);
					p = new ProductVO();
					db.executePopulate(p, rs);
				}
				ProductAllianceVO a = new ProductAllianceVO();
				a.setAllyId(rs.getString("ALLY_ID"));
				p.addAlliance(a);

				String sectionId = rs.getString("SECTION_ID");
				if (StringUtil.isEmpty(sectionId))continue;
				// Add the acl for the product
				addAcl(sectionId, hierarchies, p);
			}

		} catch (SQLException e) {
			log.error(e);
		}
		return acls;
	}

	/**
	 * Add the supplied product to the main company and all allied companies
	 * @param p
	 * @param acls
	 */
	private void addAllProducts(ProductVO p, Map<String, List<ProductVO>> acls) {
		if (p == null) return;
		addProduct(p, Section.COMPANY.name()+"_"+p.getCompanyId(), acls);
		for (ProductAllianceVO a : p.getAlliances())
			addProduct(p, Section.COMPANY.name()+"_"+a.getAllyId(), acls);
	}

	/**
	 * Ensure that there is a list in the map for the supplied company and add the product
	 * @param p
	 * @param companyId
	 * @param products
	 */
	private void addProduct(ProductVO p, String companyId, Map<String, List<ProductVO>> products) {
		if (!products.containsKey(companyId))
			products.put(companyId, new ArrayList<>());

		products.get(companyId).add(p);
	}

	/**
	 * Add the requested section to the list associated with the supplied company
	 * @param sectionId
	 * @param hierarchies
	 * @param companyId
	 * @param acls
	 */
	private void addAcl(String sectionId, SmarttrakTree hierarchies, ProductVO p) {
		Node n = hierarchies.findNode(sectionId);
		if (n == null) return;
		SectionVO sec = (SectionVO)n.getUserObject();
		p.addProductSection(sec);
	}


	/**
	 * Build the product sql
	 * @param id
	 * @return
	 */
	private String getProductSql(String id) {
		StringBuilder sql = new StringBuilder(300);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT p.PRODUCT_NM, p.SHORT_NM, p.ALIAS_NM, p.PRODUCT_ID, p.COMPANY_ID, ps.SECTION_ID, a.COMPANY_ID as ALLY_ID FROM ");
		sql.append(customDb).append("BIOMEDGPS_PRODUCT p ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("BIOMEDGPS_PRODUCT_SECTION ps ");
		sql.append("ON ps.PRODUCT_ID = p.PRODUCT_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("biomedgps_product_alliance_xr a ");
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
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("BIOMEDGPS_COMPANY_ATTRIBUTE a on a.ATTRIBUTE_ID = x.ATTRIBUTE_ID ");
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
		company.addAttribute("ticker", rs.getString("stock_abbr_txt"));
		company.setDocumentUrl(AdminControllerAction.Section.COMPANY.getPageURL()+config.getProperty(Constants.QS_PATH)+rs.getString(COMPANY_ID));
		company.addAttribute("productCount", rs.getInt("PRODUCT_NO"));
		SmarttrakSolrUtil.setSearchField(rs.getString("PARENT_NM"), "parentNm", company);
		SmarttrakSolrUtil.setSearchField(rs.getString("alias_nm"), "aliasNm", company);

		//concat some fields into meta-keywords
		StringBuilder sb = new StringBuilder(100);
		sb.append(StringUtil.checkVal(rs.getString("SHORT_NM_TXT")));
		if (sb.length() > 0) sb.append(", ");
		sb.append(StringUtil.checkVal(rs.getString("stock_abbr_txt")));
		if (sb.length() > 0) sb.append(", ");
		sb.append(StringUtil.checkVal(rs.getString("alias_nm")));
		company.setMetaKeywords(sb.toString());


		if (rs.getTimestamp("UPDATE_DT") != null) {
			company.setUpdateDt(rs.getDate("UPDATE_DT"));
		} else {
			company.setUpdateDt(rs.getDate("CREATE_DT"));
		}
		company.addOrganization(ORG_ID);
		if (1 == rs.getInt("PUBLIC_FLG")) {
			company.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
		} else if (!"P".equals(rs.getString("STATUS_NO"))) {
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
		sql.append("SELECT c.COMPANY_ID, a.SECTION_ID, c.COMPANY_NM, c.STATUS_NO, c.stock_abbr_txt, c.PUBLIC_FLG, c.SHORT_NM_TXT, ");
		sql.append("c2.COMPANY_NM as PARENT_NM, COUNT(p.COMPANY_ID) as PRODUCT_NO, c.CREATE_DT, c.UPDATE_DT, c.alias_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("BIOMEDGPS_PRODUCT p ");
		sql.append("ON p.COMPANY_ID = c.COMPANY_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("BIOMEDGPS_COMPANY_ATTRIBUTE_XR xr ");
		sql.append("ON xr.COMPANY_ID = c.COMPANY_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("BIOMEDGPS_COMPANY_ATTRIBUTE a ");
		sql.append("ON a.ATTRIBUTE_ID = xr.ATTRIBUTE_ID and a.SECTION_ID is not null ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("BIOMEDGPS_COMPANY_SECTION cs ");
		sql.append("ON cs.COMPANY_ID = c.COMPANY_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("BIOMEDGPS_COMPANY c2 ");
		sql.append("ON c2.COMPANY_ID = c.PARENT_ID ");
		sql.append("WHERE 1=1 ");
		if (id != null) sql.append("and c.COMPANY_ID = ? ");
		sql.append("GROUP BY c.COMPANY_ID, c.COMPANY_NM, a.SECTION_ID, c.STATUS_NO, ");
		sql.append("c.stock_abbr_txt, p.COMPANY_ID, c2.COMPANY_NM, c.CREATE_DT, c.UPDATE_DT ");
		sql.append("order by c.company_id ");
		log.debug(sql);
		return sql.toString();
	}


	/**
	 * Get a full hierarchy list
	 * @return
	 */
	private SmarttrakTree createHierarchies() {
		SectionHierarchyAction sha = new SectionHierarchyAction();
		sha.setAttributes(getAttributes());
		sha.setDBConnection(new SMTDBConnection(dbConn));
		//building our own Tree here preserves the root node (as a parent of MASTER_ROOT) 
		SmarttrakTree t = new SmarttrakTree(sha.getHierarchy());
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
		sql.append(DBUtil.SELECT_FROM_STAR).append(customDb).append("BIOMEDGPS_COMPANY_LOCATION l ");
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