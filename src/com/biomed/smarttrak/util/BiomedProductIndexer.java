package com.biomed.smarttrak.util;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;

import com.biomed.smarttrak.vo.ProductAllianceVO;
import com.biomed.smarttrak.vo.ProductAttributeVO;
import com.biomed.smarttrak.vo.RegulationVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: BiomedProductIndexer.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Index all products.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Feb 15, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class BiomedProductIndexer  extends SMTAbstractIndex {
	private static final String ORG_ID = "BMG_SMARTTRAK";
	public static final String INDEX_TYPE = "BIOMEDGPS_PRODUCT";
	private static final String DETAILS_ROOT = "DETAILS_ROOT";
	private static final String US_REGION_ID = "1";

	public BiomedProductIndexer(Properties config) {
		this.config = config;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	@SuppressWarnings("resource")
	@Override
	public void addIndexItems(SolrClient server) {
		SolrActionUtil solrUtil = new SolrActionUtil(server);
		Map<String, SolrDocumentVO> products = retreiveProducts(null);
		
		// Loop over each form transaction and turn it into a SolrStoryVO for processing
		for (Entry<String, SolrDocumentVO> entry : products.entrySet()) {
			try {
				solrUtil.addDocument(entry.getValue());
			} catch (Exception e) {
				log.error("could add to Solr", e);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, SolrDocumentVO> retreiveProducts(String id) {
		Map<String, SolrDocumentVO> products = new HashMap<>();
		String sql = buildRetrieveSql(id);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			if (id != null) ps.setString(1, id);
			
			ResultSet rs = ps.executeQuery();
			String currentProduct = "";
			SolrDocumentVO product = null;
			while (rs.next()) {
				if (!currentProduct.equals(rs.getString("PRODUCT_ID"))) {
					if (product != null) {
						products.put(product.getDocumentId(), product);
					}
					product = buildSolrDocument(rs);
					currentProduct = rs.getString("PRODUCT_ID");
				}
				if (rs.getString("SECTION_ID") != null && product != null) {
					product.addSection(rs.getString("SECTION_ID"));
					if (!product.getAttributes().keySet().contains("sectionName")) {
						product.addAttribute("sectionName", new ArrayList<String>());
					}
					((List<String>)product.getAttribute("sectionName")).add(rs.getString("SECTION_NM"));
				}
				
			}
			if (product != null) {
				products.put(product.getDocumentId(), product);
			}
		} catch (SQLException e) {
			log.error(e);
		}
		buildDetails(products);
		buildRegulatory(products);
		buildAlliances(products);
		
		return products;
	}
	
	
	/**
	 * Add all alliances to the products
	 * @param products
	 */
	@SuppressWarnings("unchecked")
	protected void buildAlliances(Map<String, SolrDocumentVO> products) {
		Map<String, List<ProductAllianceVO>> alliances = retrieveAlliances();
		for (Entry<String, List<ProductAllianceVO>> entry : alliances.entrySet()) {
			SolrDocumentVO p = products.get(entry.getKey());
			if (p == null) log.error("Failed to get product " + entry.getKey());
			p.addAttribute("ally", new ArrayList<String>());
			p.addAttribute("alliance", new ArrayList<String>());
			p.addAttribute("allyId", new ArrayList<String>());
			p.addAttribute("allianceId", new ArrayList<String>());
			for (ProductAllianceVO alliance : entry.getValue()) {
				((List<String>)p.getAttribute("ally")).add(alliance.getAllyName());
				((List<String>)p.getAttribute("alliance")).add(alliance.getAllianceTypeName());
				((List<String>)p.getAttribute("allyId")).add(alliance.getAllyId());
				((List<String>)p.getAttribute("allianceId")).add(alliance.getAllianceTypeId());
			}
		}
	}

	
	/**
	 * Get all alliances from the database and put them into a map with a
	 * product id key
	 * @return
	 */
	protected Map<String, List<ProductAllianceVO>> retrieveAlliances() {
		StringBuilder sql = new StringBuilder(475);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_PRODUCT_ALLIANCE_XR xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_ALLIANCE_TYPE t ");
		sql.append("on t.ALLIANCE_TYPE_ID = xr.ALLIANCE_TYPE_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("on c.COMPANY_ID = xr.COMPANY_ID ");
		
		DBProcessor db = new DBProcessor(dbConn);
		
		List<Object> results = db.executeSelect(sql.toString(), null, new ProductAllianceVO());
		Map<String, List<ProductAllianceVO>> alliances = new HashMap<>();
		for (Object o : results) {
			ProductAllianceVO vo = (ProductAllianceVO) o;
			if (!alliances.containsKey(vo.getProductId())) {
				alliances.put(vo.getProductId(), new ArrayList<ProductAllianceVO>());
			}
			alliances.get(vo.getProductId()).add(vo);
		}
		
		return alliances;
	}

	/**
	 * Add regulatory information to all products
	 * @param products
	 */
	@SuppressWarnings("unchecked")
	private void buildRegulatory(Map<String, SolrDocumentVO> products) {
		Map<String, List<RegulationVO>> regulatoryList = retrieveRegulatory();
		for (Entry<String, List<RegulationVO>> entry : regulatoryList.entrySet()) {
			SolrDocumentVO p = products.get(entry.getKey());
			p.addAttribute("usPathNm", new ArrayList<String>());
			p.addAttribute("usStatusNm", new ArrayList<String>());
			p.addAttribute("intRegionNm", new ArrayList<String>());
			p.addAttribute("intStatusNm", new ArrayList<String>());
			p.addAttribute("intPathNm", new ArrayList<String>());
			p.addAttribute("usPathId", new ArrayList<String>());
			p.addAttribute("usStatusId", new ArrayList<String>());
			p.addAttribute("intRegionId", new ArrayList<String>());
			p.addAttribute("intStatusId", new ArrayList<String>());
			p.addAttribute("intPathId", new ArrayList<String>());
			for (RegulationVO reg : entry.getValue()) {
				if (US_REGION_ID.equals(reg.getRegionId())) {
					((List<String>)p.getAttribute("usPathNm")).add(reg.getPathName());
					((List<String>)p.getAttribute("usStatusNm")).add(reg.getStatusName());
					((List<String>)p.getAttribute("usPathId")).add(reg.getPathId());
					((List<String>)p.getAttribute("usStatusId")).add(reg.getStatusId());
				} else {
					((List<String>)p.getAttribute("intPathNm")).add(reg.getPathName());
					((List<String>)p.getAttribute("intStatusNm")).add(reg.getStatusName());
					((List<String>)p.getAttribute("intRegionNm")).add(reg.getRegionName());
					((List<String>)p.getAttribute("intRegionId")).add(reg.getRegionId());
					((List<String>)p.getAttribute("intPathId")).add(reg.getPathId());
					((List<String>)p.getAttribute("intStatusId")).add(reg.getStatusId());
				}
			}
		}
		
	}

	
	/**
	 * Get all regulatory information
	 * @return
	 */
	private Map<String, List<RegulationVO>> retrieveRegulatory() {
		StringBuilder sql = new StringBuilder(475);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_PRODUCT_REGULATORY r ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_REGULATORY_STATUS s ");
		sql.append("ON s.STATUS_ID = r.STATUS_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_REGULATORY_REGION re ");
		sql.append("ON re.REGION_ID = r.REGION_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_REGULATORY_PATH p ");
		sql.append("ON p.PATH_ID = r.PATH_ID ");
		
		DBProcessor db = new DBProcessor(dbConn);
		
		List<Object> results = db.executeSelect(sql.toString(), null, new RegulationVO());
		Map<String, List<RegulationVO>> regulations = new HashMap<>();
		for (Object o : results) {
			RegulationVO vo = (RegulationVO) o;
			if (!regulations.containsKey(vo.getProductId())) {
				regulations.put(vo.getProductId(), new ArrayList<RegulationVO>());
			}
			regulations.get(vo.getProductId()).add(vo);
		}
		
		return regulations;
	}

	
	/**
	 * Add the details to the product
	 * @param products
	 */
	@SuppressWarnings("unchecked")
	private void buildDetails(Map<String, SolrDocumentVO> products) {
		Tree t = retrieveAttributes();
		// The root node contains the root of the details attributes.
		for (Node parent : t.findNode(DETAILS_ROOT).getChildren()) {
			// The first set of children compose the groupings of the
			// detail attributes and make up the names of the solr fields
			for (Node child : parent.getChildren()) {
				// Nodes at this level represent the possible selected options for
				// the detail groupings. Each one must be looped over and added to the proper
				// product in the correct grouping.
				List<ProductAttributeVO> attrs = (List<ProductAttributeVO>) child.getUserObject();
				for (ProductAttributeVO attr : attrs) {
					if (attr.getProductId() == null) continue;
					SolrDocumentVO p = products.get(attr.getProductId());
					if (!p.getAttributes().containsKey(parent.getNodeName())) {
						// Two fields are needed for the details, one to search against
						// and one to display in the product explorer
						p.addAttribute(parent.getNodeName(), new ArrayList<String>());
						p.addAttribute(parent.getNodeName() + "Ids", new ArrayList<String>());
					}
					((List<String>)p.getAttribute(parent.getNodeName())).add(child.getNodeName());
					((List<String>)p.getAttribute(parent.getNodeName()+"Ids")).add(child.getNodeId());
					
				}
			}
		}
	}
	
	
	/**
	 * Build a solr document out of the supplied result set row
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private SolrDocumentVO buildSolrDocument(ResultSet rs) throws SQLException {
		SolrDocumentVO product = new SolrDocumentVO(INDEX_TYPE);
		product.setDocumentId(rs.getString("PRODUCT_ID"));
		product.setTitle(rs.getString("PRODUCT_NM"));
		product.setContentType(rs.getString("STATUS_NO"));
		product.addAttribute("company", rs.getString("COMPANY_NM"));
		product.addAttribute("companyId", rs.getString("COMPANY_ID"));
		product.addAttribute("alias", rs.getString("ALIAS_NM"));
		
		if (rs.getTimestamp("UPDATE_DT") != null) {
			product.setUpdateDt(rs.getDate("UPDATE_DT"));
		} else {
			product.setUpdateDt(rs.getDate("CREATE_DT"));
		}
		product.addOrganization(ORG_ID);
		product.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
		
		return product;
	}
	
	
	/**
	 * Retrieve all attributes.
	 * @return
	 */
	private Tree retrieveAttributes() {
		StringBuilder sql = new StringBuilder(125);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE a ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE_XR x ");
		sql.append("ON a.ATTRIBUTE_ID = x.ATTRIBUTE_ID ");
		sql.append("ORDER BY a.ATTRIBUTE_ID ");
		List<Node> nodes = new ArrayList<>();
		DBProcessor db = new DBProcessor(dbConn);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			String curr = "";
			List<ProductAttributeVO> attrList = null;
			Node n = null;
			while(rs.next()) {
				if (!curr.equals(rs.getString("ATTRIBUTE_ID"))) {
					if (n != null) {
						n.setUserObject(attrList);
						nodes.add(n);
					}
					n = new Node(rs.getString("ATTRIBUTE_ID"), rs.getString("PARENT_ID"));
					n.setNodeName(rs.getString("ATTRIBUTE_NM"));
					attrList = new ArrayList<>();
					curr = rs.getString("ATTRIBUTE_ID");
				}
				ProductAttributeVO attr = new ProductAttributeVO();
				db.executePopulate(attr, rs);
				attrList.add(attr);
			}
			if (n != null) {
				n.setUserObject(attrList);
				nodes.add(n);
			}
		} catch (SQLException e) {
			log.error(e);
		}
		Tree t = new Tree(nodes);
		
		return t;
	}

	
	/**
	 * Create the sql for the product retrieve
	 * @param id
	 * @return
	 */
	private String buildRetrieveSql(String id) {
		StringBuilder sql = new StringBuilder(275);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT p.*, s.SECTION_ID, s.SECTION_NM, c.COMPANY_NM FROM ").append(customDb).append("BIOMEDGPS_PRODUCT p ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT_SECTION ps ");
		sql.append("ON ps.PRODUCT_ID = p.PRODUCT_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_SECTION s ");
		sql.append("ON ps.SECTION_ID = s.SECTION_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("ON c.COMPANY_ID = p.COMPANY_ID ");
		if (id != null) sql.append("WHERE p.PRODUCT_ID = ? ");
		log.info(sql);
		return sql.toString();
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
		Map<String, SolrDocumentVO> company = retreiveProducts(id);
		try (SolrActionUtil util = new SolrActionUtil(super.makeServer())) {
			for (Entry<String, SolrDocumentVO> entry : company.entrySet()) {
				util.addDocument(entry.getValue());
			}
		} catch (Exception e) {
			log.error("Failed to update company with id: " + id, e);
		}
	}
}