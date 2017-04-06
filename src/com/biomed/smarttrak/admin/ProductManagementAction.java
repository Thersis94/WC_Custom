package com.biomed.smarttrak.admin;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.biomed.smarttrak.util.BiomedProductIndexer;
import com.biomed.smarttrak.vo.ProductAllianceVO;
import com.biomed.smarttrak.vo.ProductAttributeTypeVO;
import com.biomed.smarttrak.vo.ProductAttributeVO;
import com.biomed.smarttrak.vo.ProductVO;
import com.biomed.smarttrak.vo.RegulationVO;
import com.biomed.smarttrak.vo.SectionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>: ProductManagementAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Manages products for the biomed gps site.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Feb 1, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class ProductManagementAction extends AbstractTreeAction {
	
	public static final String ACTION_TARGET = "actionTarget";
	
	public static final String DETAILS_ID = "DETAILS_ROOT";
	
	private enum ActionTarget {
		PRODUCT, PRODUCTATTRIBUTE, ATTRIBUTE, 
		ATTRIBUTELIST, ALLIANCE, DETAILSATTRIBUTE, REGULATION
	}

	
	/**
	 * Enum for handling sort values passed to the action
	 * by the bootstrap table
	 */
	private enum SortField {
		productName("PRODUCT_NM"),
		statusNo("p.STATUS_NO"),
		companyName("COMPANY_NM"),
		orderNo("p.ORDER_NO");
		
		private String dbField;
		
		SortField(String dbField) {
			this.dbField = dbField;
		}
		
		public String getDbField() {
			return dbField;
		}
	}

	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	
	@Override
	public void delete(ActionRequest req) throws ActionException {
		deleteElement(req);
	}
	
	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		ActionTarget action;
		
		if (req.hasParameter(ACTION_TARGET)) {
			action = ActionTarget.valueOf(req.getParameter(ACTION_TARGET));
		} else {
			action = ActionTarget.PRODUCT;
		}
		
		switch (action) {
			case PRODUCT:
				retrieveProduct(req);
				break;
			case PRODUCTATTRIBUTE:
				productAttributeRetrieve(req);
				break;
			case ATTRIBUTE:
				attributeRetrieve(req);
				break;
			case ATTRIBUTELIST:
				super.putModuleData(getProductAttributes(req.getParameter("productId")));
				break;
			case ALLIANCE:
				allianceRetrieve(req);
				break;
			case DETAILSATTRIBUTE:
				retrieveModuleSets(req);
				break;
			case REGULATION:
				retrieveRegulatory(req);
				break;
		}
	}
	
	
	/**
	 * Get regulations associated with a product or an id
	 */
	protected void retrieveRegulatory(ActionRequest req) {
		StringBuilder sql = new StringBuilder(475);
		String customDb = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		List<Object> params = new ArrayList<>();
		
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_PRODUCT_REGULATORY r ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_REGULATORY_STATUS s ");
		sql.append("ON s.STATUS_ID = r.STATUS_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_REGULATORY_REGION re ");
		sql.append("ON re.REGION_ID = r.REGION_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_REGULATORY_PATH p ");
		sql.append("ON p.PATH_ID = r.PATH_ID ");
		sql.append("WHERE r.PRODUCT_ID = ? ");
		params.add(req.getParameter("productId"));
		if (req.hasParameter("regulatoryId")) {
			sql.append("and r.REGULATORY_ID = ? ");
			params.add(req.getParameter("regulatoryId"));
		}
		
		DBProcessor db = new DBProcessor(dbConn);
		
		List<Object> results = db.executeSelect(sql.toString(), params, new RegulationVO());
		
		if (results.isEmpty()) {
			super.putModuleData(new RegulationVO());
		} else {
			super.putModuleData(results.get(0));
		}
	}


	/**
	 * Get information neccesary to populate product attribute pages
	 * @param req
	 */
	private void productAttributeRetrieve(ActionRequest req) {
		if (req.hasParameter("productAttributeId"))
			retrieveProductAttribute(req);
		req.setParameter("getList", "true");
		retrieveAttributes(req);
	}


	/**
	 * Determine what kind of attribute data needs to be retrieved and do so.
	 * @param req
	 */
	protected void attributeRetrieve(ActionRequest req) {
		if (req.hasParameter("attributeId")) {
			retrieveAttribute(req.getParameter("attributeId"));
		} else if (!req.hasParameter("add")){
			retrieveAttributes(req);
		}
	}


	/**
	 * Get all available module sets and flag all that are assigned to a product
	 * @param req
	 * @throws ActionException
	 */
	protected void retrieveModuleSets(ActionRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(550);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select a.attribute_id, a.parent_id, a.attribute_nm, string_agg(pm.moduleset_id, ',') as section_ids ");
		sql.append("from ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE a ");
		sql.append("left join ").append(customDb).append("BIOMEDGPS_PRODUCT_MODULESET_XR xr ");
		sql.append("on xr.attribute_id = a.attribute_id ");
		sql.append("left join ").append(customDb).append("BIOMEDGPS_PRODUCT_MODULESET pm ");
		sql.append("on pm.moduleset_id = xr.moduleset_id ");
		sql.append("group by a.attribute_id, a.parent_id, a.attribute_nm ");
		sql.append("order by a.order_no ");
		log.debug(sql);
		List<Node> attributes = new ArrayList<>();
		
		List<String> activeDetails = getAssingedAttributes(req.getParameter("productId"));
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				ProductAttributeTypeVO attr = new ProductAttributeTypeVO();
				attr.setAttributeId(rs.getString("attribute_id"));
				attr.setAttributeName(rs.getString("attribute_nm"));
				attr.addSectionIds(rs.getString("section_ids"));
				attr.setParentId(rs.getString("parent_id"));
				if (activeDetails.contains(attr.getAttributeId()))
					attr.setActiveFlag(1);
				Node n = new Node(attr.getAttributeId(), attr.getParentId());
				n.setUserObject(attr);
				attributes.add(n);
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
		
		Tree t = new Tree(attributes);
		t.setRootNode(t.findNode(req.getParameter("attributeId")));
		super.putModuleData(t);
		
	}


	/**
	 * Get a list of all assigned attributes for a product
	 * @param productId
	 * @return
	 * @throws ActionException
	 */
	protected List<String> getAssingedAttributes(String productId) throws ActionException {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select ATTRIBUTE_ID from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_PRODUCT_ATTRIBUTE_XR where PRODUCT_ID = ? ");
		List<String> activeDetails = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, productId);
			
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				activeDetails.add(rs.getString("ATTRIBUTE_ID"));
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
		
		return activeDetails;
	}


	/**
	 * Determine how to retrieve product information and do so.
	 * @param req
	 * @throws ActionException
	 */
	protected void allianceRetrieve(ActionRequest req) throws ActionException {
		if (req.hasParameter("allianceId"))
			retrieveAlliance(req.getParameter("allianceId"));
	}


	/**
	 * Retrieve all information pertaining to a particular alliance
	 * @param allianceId
	 */
	protected void retrieveAlliance(String allianceId) {
		StringBuilder sql = new StringBuilder(100);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_PRODUCT_ALLIANCE_XR ");
		sql.append("WHERE PRODUCT_ALLIANCE_XR_ID = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(allianceId);
		DBProcessor db = new DBProcessor(dbConn);
		ProductAllianceVO alliance = (ProductAllianceVO) db.executeSelect(sql.toString(), params, new ProductAllianceVO()).get(0);
		super.putModuleData(alliance);
	}
	
	
	/**
	 * Determine whether to get one product or all and do so.
	 * @param req
	 * @throws ActionException
	 */
	protected void retrieveProduct(ActionRequest req) throws ActionException {
		if (req.hasParameter("productId") && ! req.hasParameter("add")) {
			retrieveProduct(req.getParameter("productId"), req);
		} else if (!req.hasParameter("add")) {
			retrieveProducts(req);
		} else if (req.getSession().getAttribute("hierarchyTree") == null){
			// This is a form for a new market make sure that the hierarchy tree is present 
			Tree t = loadDefaultTree();
			req.getSession().setAttribute("hierarchyTree", t.preorderList());
		}
	}


	/**
	 * get a particular product attribute from the database for editing
	 * @param req
	 */
	protected void retrieveProductAttribute(ActionRequest req) {
		StringBuilder sql = new StringBuilder(300);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT xr.*, a.TYPE_CD FROM ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE_XR xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE a ");
		sql.append("ON a.ATTRIBUTE_ID = xr.ATTRIBUTE_ID ");
		sql.append("WHERE PRODUCT_ATTRIBUTE_ID = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(req.getParameter("productAttributeId"));
		DBProcessor db = new DBProcessor(dbConn);
		ProductAttributeVO attr = (ProductAttributeVO) db.executeSelect(sql.toString(), params, new ProductAttributeVO()).get(0);
		super.putModuleData(attr);
		req.setParameter("rootNode", attr.getAttributeId());
	}
	
	
	/**
	 *Retrieve all attributes available to the product.
	 * @param req
	 */
	protected void retrieveAttributes(ActionRequest req) {
		StringBuilder sql = new StringBuilder(100);
		List<Object> params = new ArrayList<>();
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_PRODUCT_ATTRIBUTE ");
		if (req.hasParameter("search")) {
			sql.append("WHERE lower(ATTRIBUTE_NM) like ? ");
			params.add("%" + req.getParameter("search").toLowerCase() + "%");
		}
		if (req.hasParameter("attributeTypeCd")) {
			sql.append("WHERE TYPE_CD = ? ");
			params.add(req.getParameter("attributeTypeCd"));
		}
		
		sql.append("ORDER BY ORDER_NO ");
		log.debug(sql);
		DBProcessor db = new DBProcessor(dbConn);
		List<Object> results = db.executeSelect(sql.toString(), params, new ProductAttributeTypeVO());
		List<Node> orderedResults = new ArrayList<>();
		for (Object o : results) {
			ProductAttributeTypeVO attr = (ProductAttributeTypeVO)o;
			Node n = new Node(attr.getAttributeId(), attr.getParentId());
			n.setUserObject(attr);
			orderedResults.add(n);
		}
		storeAttributeData(req, orderedResults);
	}


	/**
	 * Determine where and how much attribute data to save.
	 * @param req
	 * @param orderedResults
	 */
	private void storeAttributeData(ActionRequest req,
			List<Node> orderedResults) {

		int rpp = Convert.formatInteger(req.getParameter("limit"), 10);
		int page = Convert.formatInteger(req.getParameter("offset"), 0)/rpp;
		int end = orderedResults.size() < rpp*(page+1)? orderedResults.size() : rpp*(page+1);
		
		// If all attributes of a type is being requested set it as a request attribute since it is
		// being used to supplement the attribute xr editing.
		// Search data should not be turned into a tree after a search as requisite nodes may be missing
		if (req.hasParameter("attributeTypeCd") || Convert.formatBoolean(req.getParameter("getList"))) {
			Tree t = new Tree(orderedResults);
			Node rootNode = null;
			if (req.hasParameter("rootNode")) {
				rootNode = t.findNode(req.getParameter("rootNode"));
				if (rootNode.getParentId() != null) {
					rootNode = getTopParent(t, rootNode);
				}
			}
			
			if (rootNode != null && rootNode.getNumberChildren() > 0) {
				req.getSession().setAttribute("attributeList", t.preorderList(rootNode));
			} else {
				req.getSession().setAttribute("attributeList", t.preorderList());
			}

		} else if (req.hasParameter("search")) {
			super.putModuleData(orderedResults.subList(rpp*page, end), orderedResults.size(), false);
		} else {
			super.putModuleData(new Tree(orderedResults).getPreorderList().subList(rpp*page, end), orderedResults.size(), false);
		}
	}


	/**
	 * Ensure that we are getting a top level root node.
	 * @param t
	 * @param rootNode
	 * @return
	 */
	protected Node getTopParent(Tree t, Node rootNode) {
		if (rootNode.getParentId() != null) {
			return getTopParent(t, t.findNode(rootNode.getParentId()));
		}
		return rootNode;
	}


	/**
	 * Get the details of the supplied attribute type
	 * @param attributeId
	 */
	protected void retrieveAttribute(String attributeId) {
		StringBuilder sql = new StringBuilder(100);
		List<Object> params = new ArrayList<>();
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_PRODUCT_ATTRIBUTE ");
		sql.append("WHERE ATTRIBUTE_ID = ? ");
		params.add(attributeId);
		log.debug(sql);
		DBProcessor db = new DBProcessor(dbConn);
		List<Object> res = db.executeSelect(sql.toString(), params, new ProductAttributeTypeVO());

		if (!res.isEmpty()) {
			ProductAttributeTypeVO attr = (ProductAttributeTypeVO) db.executeSelect(sql.toString(), params, new ProductAttributeTypeVO()).get(0);
			super.putModuleData(attr);
		} else {
			super.putModuleData(new ProductAttributeTypeVO());
		}
	}

	
	/**
	 * Retrieve all companies from the database as well as create a flag that 
	 * shows whether the product has been invested in by another product.
	 * @param req
	 * @throws ActionException
	 */
	protected void retrieveProducts(ActionRequest req) throws ActionException {
		List<Object> params = new ArrayList<>();
		String customDb = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		sql.append("select * ").append("FROM ").append(customDb).append("BIOMEDGPS_product p ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("ON c.COMPANY_ID = p.COMPANY_ID ");
		
		// If the request has search terms on it add them here
		if (req.hasParameter("search")) {
			sql.append("WHERE lower(PRODUCT_NM) like ?");
			params.add("%" + req.getParameter("search").toLowerCase() + "% ");
		}
		
		SortField s;
		if (req.hasParameter("sort")) {
			s = SortField.valueOf(req.getParameter("sort"));
		} else {
			s = SortField.productName;
		}
		sql.append("ORDER BY ").append(s.getDbField());
		sql.append(" ").append(req.hasParameter("order")? req.getParameter("order"):"desc").append(" ");
		
		int limit  = Convert.formatInteger(req.getParameter("limit"));
		if (limit != 0) {
			sql.append("LIMIT ? OFFSET ? ");
			params.add(Convert.formatInteger(req.getParameter("limit")));
			params.add(Convert.formatInteger(req.getParameter("offset")));
		}
		log.debug(sql);
		
		DBProcessor db = new DBProcessor(dbConn);
		List<Object> products = db.executeSelect(sql.toString(), params, new ProductVO());
		super.putModuleData(products, getProductCount(req.getParameter("searchData")), false);
	}

	
	/**
	 * Get a count of how many products are in the database
	 * @return
	 * @throws ActionException 
	 */
	protected int getProductCount(String searchData) throws ActionException {
		String customDb = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(150);
		sql.append("select COUNT(*) ").append("FROM ").append(customDb).append("BIOMEDGPS_product p ");
		// If the request has search terms on it add them here
		if (!StringUtil.isEmpty(searchData)) {
			sql.append("WHERE lower(PRODUCT_NM) like ?");
		}
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			if (!StringUtil.isEmpty(searchData)) ps.setString(1, "%" + searchData.toLowerCase() + "%");
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return rs.getInt(1);
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		
		return 0;
	}

	
	/**
	 * Get all information related to the supplied product.
	 * @param productId
	 * @throws ActionException
	 */
	protected void retrieveProduct(String productId, ActionRequest req) throws ActionException {
		ProductVO product;
		StringBuilder sql = new StringBuilder(100);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_PRODUCT ");
		sql.append("WHERE PRODUCT_ID = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(productId);
		DBProcessor db = new DBProcessor(dbConn);
		product = (ProductVO) db.executeSelect(sql.toString(), params, new ProductVO()).get(0);

		Tree t = loadDefaultTree();
		
		req.getSession().setAttribute("hierarchyTree", t.preorderList());
		req.getSession().setAttribute("productName", product.getProductName());

		if ("alliance".equals(req.getParameter("jsonType")))
			addAlliances(product);
		if ("attribute".equals(req.getParameter("jsonType")))
			addAttributes(product);
		if ("regulation".equals(req.getParameter("jsonType")))
			addRegulations(product);
		
		getActiveSections(product);
		super.putModuleData(product);
	}
	
	
	/**
	 * Get all regulations associated to this product and add them
	 * @param product
	 */
	protected void addRegulations(ProductVO product) {
		StringBuilder sql = new StringBuilder(475);
		String customDb = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_PRODUCT_REGULATORY r ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_REGULATORY_STATUS s ");
		sql.append("ON s.STATUS_ID = r.STATUS_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_REGULATORY_REGION re ");
		sql.append("ON re.REGION_ID = r.REGION_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_REGULATORY_PATH p ");
		sql.append("ON p.PATH_ID = r.PATH_ID ");
		sql.append("WHERE r.PRODUCT_ID = ? ");
		log.debug(sql);
		List<Object> params = new ArrayList<>();
		params.add(product.getProductId());
		
		DBProcessor db = new DBProcessor(dbConn);
		
		List<Object> results = db.executeSelect(sql.toString(), params, new RegulationVO());
		
		for (Object o : results) product.addRegulation((RegulationVO)o);
	}


	/**
	 * Get all alliances the supplied product is in and add them to the vo
	 * @param product
	 */
	protected void addAlliances(ProductVO product) {
		StringBuilder sql = new StringBuilder(525);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_PRODUCT_ALLIANCE_XR pax ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_ALLIANCE_TYPE at ");
		sql.append("ON pax.ALLIANCE_TYPE_ID = at.ALLIANCE_TYPE_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT p  ");
		sql.append("ON p.PRODUCT_ID = pax.PRODUCT_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("ON c.COMPANY_ID = pax.COMPANY_ID ");
		sql.append("WHERE pax.PRODUCT_ID = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(product.getProductId());
		DBProcessor db = new DBProcessor(dbConn);
		
		// DBProcessor returns a list of objects that need to be individually cast to alliances
		List<Object> results = db.executeSelect(sql.toString(), params, new ProductAllianceVO());
		for (Object o : results) {
			product.addAlliance((ProductAllianceVO)o);
		}
	}
	
	
	/**
	 * Get all the sections that are associated with the supplied product
	 * @param product
	 * @throws ActionException
	 */
	protected void addSections(ProductVO product) throws ActionException {
		StringBuilder sql = new StringBuilder(275);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT SECTION_NM, xr.PRODUCT_SECTION_XR_ID, xr.SECTION_ID FROM ").append(customDb).append("BIOMEDGPS_PRODUCT_SECTION xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_SECTION s ");
		sql.append("ON s.SECTION_ID = xr.SECTION_ID ");
		sql.append("WHERE PRODUCT_ID = ? ");

		Tree t = loadDefaultTree();
		t.buildNodePaths();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, product.getProductId());
			
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				SectionVO sec = new SectionVO(rs);
				sec.setSectionId(rs.getString("PRODUCT_SECTION_XR_ID"));
				sec.setSectionNm(rs.getString("SECTION_NM"));
				setGroupName(t.findNode(rs.getString("SECTION_ID")), sec);
				product.addProductSection(sec);
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Set the group name based on the full path of the supplied node.
	 * @param n
	 * @param sec
	 */
	private void setGroupName(Node n, SectionVO sec) {
		if (n == null) return;
		String[] parts = n.getFullPath().split(SearchDocumentHandler.HIERARCHY_DELIMITER);
		if (parts.length < 2) {
			sec.setGroupNm(parts[0]);
		} else {
			sec.setGroupNm(parts[1]);
		}
	}


	/**
	 * Gets all sections that have been assigned to the supplied product
	 * @param productId
	 * @return
	 * @throws ActionException
	 */
	protected List<String> getActiveSections(ProductVO product) throws ActionException {
		StringBuilder sql = new StringBuilder(150);
		sql.append("SELECT SECTION_ID FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_PRODUCT_SECTION WHERE PRODUCT_ID = ? ");
		
		List<String> activeSections = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, product.getProductId());
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				product.addProductSection(new SectionVO(rs));
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
		
		return activeSections;
		
	}
	
	
	/**
	 * Get all attributes associated with the supplied product.
	 * @param product
	 * @throws ActionException 
	 */
	protected void addAttributes(ProductVO product) throws ActionException {
		List<Object> results = getProductAttributes(product.getProductId());
		Tree t = buildAttributeTree();
		
		for (Object o : results) {
			ProductAttributeVO p = (ProductAttributeVO)o;
			Node n = t.findNode(p.getAttributeId());
			String[] split = n.getFullPath().split(Tree.DEFAULT_DELIMITER);
			if ("LINK".equals(p.getAttributeTypeCd()) ||
					"ATTACH".equals(p.getAttributeTypeCd())) {
				p.setGroupName(StringUtil.capitalizePhrase(p.getAttributeName()));
			} else if (split.length >= 2) {
				p.setGroupName(split[1]);
			}
			product.addProductAttribute(p);
		}
	}
	

	/**
	 * Create the full attribute tree in order to determine the full ancestry of each attribute
	 * @return
	 * @throws ActionException
	 */
	private Tree buildAttributeTree() throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT c.ATTRIBUTE_ID, c.PARENT_ID, c.ATTRIBUTE_NM, p.ATTRIBUTE_NM as PARENT_NM ");
		sql.append("FROM ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE c ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE p ");
		sql.append("ON c.PARENT_ID = p.ATTRIBUTE_ID ");
		log.debug(sql);
		List<Node> attributes = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Node n = new Node(rs.getString("ATTRIBUTE_ID"), rs.getString("PARENT_ID"));
				if ("profile".equals(rs.getString("ATTRIBUTE_NM"))) {
					n.setNodeName(rs.getString("PARENT_NM"));
				} else {
					n.setNodeName(rs.getString("ATTRIBUTE_NM"));
				}
				attributes.add(n);
			}
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		Tree t = new Tree(attributes);
		t.buildNodePaths(t.getRootNode(), Tree.DEFAULT_DELIMITER, true);
		return t;
	}
	
	
	/**
	 * Returns a list of attributes for a product, excluding attributes that
	 * fall under the Product Details tree of attributes.
	 * @param productId
	 * @return
	 */
	protected List<Object> getProductAttributes(String productId) {
		StringBuilder sql = new StringBuilder(150);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE_XR xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE a ");
		sql.append("ON a.ATTRIBUTE_ID = xr.ATTRIBUTE_ID ");
		sql.append("WHERE PRODUCT_ID = ? AND xr.ATTRIBUTE_ID not in ( ");
		sql.append("SELECT child.ATTRIBUTE_ID from ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE child ");
		sql.append("INNER JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE parent ");
		sql.append("on parent.ATTRIBUTE_ID = child.PARENT_ID ");
		sql.append("where parent.PARENT_ID = ? ) ");
		log.debug(sql+"|"+productId);
		List<Object> params = new ArrayList<>();
		params.add(productId);
		params.add(DETAILS_ID);
		DBProcessor db = new DBProcessor(dbConn);
		
		// DBProcessor returns a list of objects that need to be individually cast to attributes
		return db.executeSelect(sql.toString(), params, new ProductAttributeVO());
	}

	
	/**
	 * Update a product or related attribute of a product
	 * @param req
	 * @throws ActionException
	 */
	protected void updateElement(ActionRequest req) throws ActionException {
		ActionTarget action = ActionTarget.valueOf(req.getParameter(ACTION_TARGET));
		DBProcessor db = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
		switch(action) {
			case PRODUCT:
				ProductVO c = new ProductVO(req);
				saveProduct(c, db);
				saveSections(req);
				break;
			case PRODUCTATTRIBUTE:
				ProductAttributeVO attr = new ProductAttributeVO(req);
				saveAttribute(attr, db);
				break;
			case ATTRIBUTE:
				ProductAttributeTypeVO t = new ProductAttributeTypeVO(req);
				saveAttributeType(t, db, Convert.formatBoolean(req.getParameter("insert")));
				break;
			case ALLIANCE:
				ProductAllianceVO a = new ProductAllianceVO(req);
				saveAlliance(a, db);
				break;
			case DETAILSATTRIBUTE:
				saveDetailsAttribute(req);
				break;
			case REGULATION:
				RegulationVO reg = new RegulationVO(req);
				saveRegulation(reg, db);
				break;
			default:break;
		}
	}


	/**
	 * Save the supplied regulation
	 * @param reg
	 * @param db
	 * @throws ActionException
	 */
	protected void saveRegulation(RegulationVO reg, DBProcessor db) throws ActionException {
		try {
			if (StringUtil.isEmpty(reg.getRegulatorId())) {
				reg.setRegulatorId(new UUIDGenerator().getUUID());
				db.insert(reg);
			} else {
				db.update(reg);
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Delete the currently saved detail attributes and save the supplied list
	 * of attributes
	 * @param req
	 * @throws ActionException
	 */
	protected void saveDetailsAttribute(ActionRequest req) throws ActionException {
		deleteCurrentDetails(req);
		
		StringBuilder sql = new StringBuilder(225);
		sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_PRODUCT_ATTRIBUTE_XR ");
		sql.append("(PRODUCT_ATTRIBUTE_ID, ATTRIBUTE_ID, PRODUCT_ID, CREATE_DT) ");
		sql.append("VALUES(?,?,?,?)");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			String productId = req.getParameter("productId");
			for (String s : req.getParameterValues("attributeId")) {
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, s);
				ps.setString(3, productId);
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Delete all attribute xrs associated with the current product that are
	 * grandchildren of the supplied root attribute id.
	 * @param req
	 * @throws ActionException
	 */
	protected void deleteCurrentDetails(ActionRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(475);
		String customDb = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("DELETE FROM ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE_XR ");
		sql.append("WHERE ATTRIBUTE_ID in ( ");
		sql.append("SELECT child.ATTRIBUTE_ID from ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE child ");
		sql.append("INNER JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE parent ");
		sql.append("on parent.ATTRIBUTE_ID = child.PARENT_ID ");
		sql.append("where parent.PARENT_ID = ? ) ");
		sql.append("and PRODUCT_ID = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, DETAILS_ID);
			ps.setString(2, req.getParameter("productId"));
			
			ps.executeUpdate();
		} catch (Exception e) {
			throw new ActionException(e);
		}
		
	}


	/**
	 * Check whether the supplied alliance needs to be updated or inserted and do so.
	 * @param a
	 * @param db
	 * @throws ActionException
	 */
	protected void saveAlliance(ProductAllianceVO a, DBProcessor db) throws ActionException {
		try {
			if (StringUtil.isEmpty(a.getAllianceId())) {
				a.setAllianceId(new UUIDGenerator().getUUID());
				db.insert(a);
			} else {
				db.update(a);
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
		
	}


	/**
	 * Add the supplied sections to the product xr table
	 * @param req
	 * @throws ActionException
	 */
	protected void saveSections(ActionRequest req) throws ActionException {
		// Delete all sections currently assigned to this product before adding
		// what is on the request object.
		deleteSection(true, req.getParameter("productId"));
		
		// If there is nothing to add return here
		if (!req.hasParameter("sectionId")) return;
		
		StringBuilder sql = new StringBuilder(225);
		sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_PRODUCT_SECTION (PRODUCT_SECTION_XR_ID, SECTION_ID, ");
		sql.append("PRODUCT_ID, CREATE_DT) ");
		sql.append("VALUES(?,?,?,?) ");
		String productId = req.getParameter("productId");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (String sectionId : req.getParameterValues("sectionId")) {
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, sectionId);
				ps.setString(3, productId);
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}

	
	/**
	 * Check whether the supplied attribute needs to be inserted or updated and do so.
	 * @param attr
	 * @param db
	 * @throws ActionException
	 */
	protected void saveAttribute(ProductAttributeVO attr, DBProcessor db) throws ActionException {
		try {
			if (StringUtil.isEmpty(attr.getProductAttributeId())) {
				attr.setProductAttributeId(new UUIDGenerator().getUUID());
				db.insert(attr);
			} else {
				db.update(attr);
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Check whether we need to insert or update the supplied vo and do so.
	 * Then update the investors for the product.
	 * @param c
	 * @param db
	 * @throws ActionException
	 */
	protected void saveProduct(ProductVO c, DBProcessor db) throws ActionException {
		try {
			if (StringUtil.isEmpty(c.getProductId())) {
				c.setProductId(new UUIDGenerator().getUUID());
					db.insert(c);
			} else {
				db.update(c);
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Check whether the supplied attribute type needs to be inserted or updated and do so.
	 * @param attr
	 * @param db
	 * @param boolean1 
	 * @throws ActionException
	 */
	protected void saveAttributeType(ProductAttributeTypeVO t, DBProcessor db, Boolean insert) throws ActionException {
		try {
			if (insert) {
				db.insert(t);
			} else {
				db.update(t);
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}

	
	/**
	 * Delete a supplied element
	 * @param req
	 * @throws ActionException
	 */
	protected void deleteElement(ActionRequest req) throws ActionException {
		ActionTarget action = ActionTarget.valueOf(req.getParameter(ACTION_TARGET));
		DBProcessor db = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
		try {
		switch(action) {
			case PRODUCT:
				ProductVO c = new ProductVO(req);
				db.delete(c);
				break;
			case PRODUCTATTRIBUTE:
				ProductAttributeVO attr = new ProductAttributeVO(req);
				db.delete(attr);
				break;
			case ATTRIBUTE:
				ProductAttributeTypeVO t = new ProductAttributeTypeVO(req);
				db.delete(t);
				break;
			case ALLIANCE:
				ProductAllianceVO a = new ProductAllianceVO(req);
				db.delete(a);
				break;
			case REGULATION:
				RegulationVO reg = new RegulationVO(req);
				db.delete(reg);
				break;
			default:break;
		}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}
	
	
	/**
	 * Delete section xrs for a product. Deletes come in single xr deletion and
	 * full wipes used when new xrs are being saved.
	 * @param full
	 * @param id
	 * @throws ActionException
	 */
	protected void deleteSection(boolean full, String id) throws ActionException {
		StringBuilder sql = new StringBuilder(150);
		sql.append("DELETE FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_PRODUCT_SECTION WHERE ");
		if (full) {
			sql.append("PRODUCT_ID = ? ");
		} else {
			sql.append("PRODUCT_SECTION_XR_ID = ? ");
		}
		log.debug(sql+"|"+id);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, id);
			
			ps.executeUpdate();
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}
	
	
	/**
	 * Take in front end requests and direct them to the proper delete or update method
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		String buildAction = req.getParameter("buildAction");
		String msg = StringUtil.capitalizePhrase(buildAction) + " completed successfully.";
		try {
			if ("update".equals(buildAction)) {			
				updateElement(req);
			} else if("delete".equals(buildAction)) {
				deleteElement(req);
			}
		} catch (Exception e) {
			msg = StringUtil.capitalizePhrase(buildAction) + " failed to complete successfully. Please contact an administrator for assistance";
		}

		String productId = req.getParameter("productId");
		if (!StringUtil.isEmpty(productId)) {
			String status = req.getParameter("statusNo");
			if (StringUtil.isEmpty(status))
				status = findStatus(productId);
			updateSolr(productId, status);
		}

		redirectRequest(msg, buildAction, req);
	}


	/**
	 * Get the status of the supplied company.
	 * @param marketId
	 * @return
	 * @throws ActionException
	 */
	protected String findStatus(String productId) throws ActionException {
		StringBuilder sql = new StringBuilder(125);
		sql.append("SELECT STATUS_NO from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_PRODUCT WHERE PRODUCT_ID = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, productId);
			
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				return rs.getString("STATUS_NO");
			}
		} catch (SQLException e) {
			throw new ActionException(e);
		}

		// If we didn't find a market with this id the action was a delete and solr needs to recognize that
		return "D";
	}



	/**
	 * Push the updates to solr
	 * @param req
	 * @param buildAction
	 * @throws ActionException
	 */
	protected void updateSolr(String productId, String status) throws ActionException {
		Properties props = new Properties();
		props.putAll(getAttributes());
		BiomedProductIndexer indexer = new BiomedProductIndexer(props);
		indexer.setDBConnection(dbConn);
		try {
			if ("D".equals(status) || "A".equals(status)) {
				indexer.purgeSingleItem(productId, false);
			} else {
				indexer.addSingleItem(productId);
			}
		} catch (IOException e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Build the redirect for build requests
	 * @param msg
	 * @param buildAction
	 * @param req
	 */
	protected void redirectRequest(String msg, String buildAction, ActionRequest req) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		// Redirect the user to the appropriate page
		StringBuilder url = new StringBuilder(128);
		url.append(page.getFullPath()).append("?actionType=productAdmin&").append("msg=").append(msg);
		
		// Only add a tab parameter if one was provided.
		if (req.hasParameter("tab")) {
			url.append("&tab=").append(req.getParameter("tab"));
		}
		//if a product is being deleted do not redirect the user to a product page
		if (!"delete".equals(buildAction) || 
				ActionTarget.valueOf(req.getParameter(ACTION_TARGET)) != ActionTarget.PRODUCT) {
			url.append("&productId=").append(req.getParameter("productId"));
		}
		
		if ("ATTRIBUTE".equals(req.getParameter(ACTION_TARGET)))
			url.append("&").append(ACTION_TARGET).append("=ATTRIBUTE");
		
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}


	@Override
	public String getCacheKey() {
		return null;
	}
}
