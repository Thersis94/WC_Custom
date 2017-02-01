package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.vo.ProductAttributeTypeVO;
import com.biomed.smarttrak.vo.ProductAttributeVO;
import com.biomed.smarttrak.vo.ProductVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

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

public class ProductManagementAction extends SimpleActionAdapter {
	
	public static final String ACTION_TARGET = "actionTarget";
	
	private enum ActionTarget {
		PRODUCT, PRODUCTATTRIBUTE, ATTRIBUTE, SECTION, ATTRIBUTELIST
	}
	
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	
	public void retrieve(ActionRequest req) throws ActionException {
		ActionTarget action;
		
		if (req.hasParameter("type")) {
			action = ActionTarget.valueOf(req.getParameter("type"));
		} else {
			action = ActionTarget.PRODUCT;
		}
		
		switch (action) {
			case PRODUCT:
				if (req.hasParameter("productId") && ! req.hasParameter("add")) {
					retrieveProduct(req.getParameter("productId"));
				} else if (!req.hasParameter("add")) {
					retrieveProducts(req);
				}
				break;
			case PRODUCTATTRIBUTE:
				if (req.hasParameter("productAttributeId"))
					retrieveProductAttribute(req);
				retrieveAttributes(req);
				break;
			case ATTRIBUTE:
				retrieveAttribute(req.getParameter("attributeId"));
				break;
			case SECTION:
				retrieveSections(req);
				break;
			case ATTRIBUTELIST:
				super.putModuleData(getProductAttributes(req.getParameter("productId")));
				break;
		}
	}
	
	
	/**
	 * get a particular product attribute from the database for editing
	 * @param req
	 */
	private void retrieveProductAttribute(ActionRequest req) {
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
		req.setParameter("attributeTypeCd", attr.getAttributeTypeCd());
		retrieveAttributes(req);
	}
	
	
	/**
	 *Retrieve all attributes available to the company.
	 * @param req
	 */
	private void retrieveAttributes(ActionRequest req) {
		StringBuilder sql = new StringBuilder(100);
		List<Object> params = new ArrayList<>();
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_PRODUCT_ATTRIBUTE ");
		if (req.hasParameter("searchData")) {
			sql.append("WHERE lower(ATTRIBUTE_NM) like ? ");
			params.add("%" + req.getParameter("searchData").toLowerCase() + "%");
		}
		if (req.hasParameter("attributeTypeCd")) {
			sql.append("WHERE TYPE_CD = ? ");
			params.add(req.getParameter("attributeTypeCd"));
		}
		
		sql.append("ORDER BY ORDER_NO ");

		DBProcessor db = new DBProcessor(dbConn);
		List<Object> results = db.executeSelect(sql.toString(), params, new ProductAttributeTypeVO());
		List<Node> orderedResults = new ArrayList<>();
		for (Object o : results) {
			ProductAttributeTypeVO attr = (ProductAttributeTypeVO)o;
			Node n = new Node(attr.getAttributeId(), attr.getParentId());
			n.setUserObject(attr);
			orderedResults.add(n);
		}

		int rpp = Convert.formatInteger(req.getParameter("rpp"), 10);
		int page = Convert.formatInteger(req.getParameter("page"), 0);
		int end = orderedResults.size() < rpp*(page+1)? orderedResults.size() : rpp*(page+1);
		
		// If all attributes of a type is being requested set it as a request attribute since it is
		// being used to supplement the attribute xr editing.
		// Search data should not be turned into a tree after a search as requisite nodes may be missing
		if (req.hasParameter("attributeTypeCd")) {
			req.getSession().setAttribute("attributeList", new Tree(orderedResults).getPreorderList());
		} else if (req.hasParameter("searchData")) {
			super.putModuleData(orderedResults.subList(rpp*page, end), orderedResults.size(), false);
		} else {
			super.putModuleData(new Tree(orderedResults).getPreorderList().subList(rpp*page, end), orderedResults.size(), false);
		}
	}


	/**
	 * Get the details of the supplied attribute type
	 * @param attributeId
	 */
	private void retrieveAttribute(String attributeId) {
		StringBuilder sql = new StringBuilder(100);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_PRODUCT_ATTRIBUTE ");
		sql.append("WHERE ATTRIBUTE_ID = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(attributeId);
		DBProcessor db = new DBProcessor(dbConn);
		List<Object> res = db.executeSelect(sql.toString(), params, new ProductAttributeTypeVO());
		if (res.size() > 0) {
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
	private void retrieveProducts(ActionRequest req) throws ActionException {
		List<Object> params = new ArrayList<>();
		String customDb = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		sql.append("select * ").append("FROM ").append(customDb).append("BIOMEDGPS_product ");
		
		// If the request has search terms on it add them here
		if (req.hasParameter("searchData")) {
			sql.append("WHERE lower(PRODUCT_NM) like ?");
			params.add("%" + req.getParameter("searchData").toLowerCase() + "%");
		}
		log.debug(sql);
		int rpp = Convert.formatInteger(req.getParameter("rpp"), 10);
		int page = Convert.formatInteger(req.getParameter("page"), 0);
		
		DBProcessor db = new DBProcessor(dbConn);
		List<Object> products = db.executeSelect(sql.toString(), params, new ProductVO());
		int end = products.size() < rpp*(page+1)? products.size() : rpp*(page+1);
		super.putModuleData(products.subList(rpp*page, end), products.size(), false);
	}

	
	/**
	 * Get all information related to the supplied product.
	 * @param productId
	 * @throws ActionException
	 */
	private void retrieveProduct(String productId) throws ActionException {
		ProductVO product;
		StringBuilder sql = new StringBuilder(100);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_PRODUCT ");
		sql.append("WHERE PRODUCT_ID = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(productId);
		DBProcessor db = new DBProcessor(dbConn);
		product = (ProductVO) db.executeSelect(sql.toString(), params, new ProductVO()).get(0);

		// Get specifics on product details
		addAttributes(product);
		addSections(product);
		
		super.putModuleData(product);
	}
	
	
	/**
	 * Get all the sections that are associated with the supplied company
	 * @param company
	 * @throws ActionException
	 */
	private void addSections(ProductVO product) throws ActionException {
		StringBuilder sql = new StringBuilder(275);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT SECTION_NM, xr.PRODUCT_SECTION_XR_ID FROM ").append(customDb).append("BIOMEDGPS_PRODUCT_SECTION xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_SECTION s ");
		sql.append("ON s.SECTION_ID = xr.SECTION_ID ");
		sql.append("WHERE PRODUCT_ID = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, product.getProductId());
			
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				product.addSection(new GenericVO(rs.getString("PRODUCT_SECTION_XR_ID"), rs.getString("SECTION_NM")));
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}
	
	
	/**
	 * Get all sections available to companies and mark the active sections
	 * @param req
	 * @throws ActionException
	 */
	private void retrieveSections(ActionRequest req) throws ActionException {
		ContentHierarchyAction c = new ContentHierarchyAction();
		c.setActionInit(actionInit);
		c.setAttributes(attributes);
		c.setDBConnection(dbConn);
		
		List<Node> hierarchy = new Tree(c.getHierarchy(null)).preorderList();
		List<String> activeNodes = getActiveSections(req.getParameter("productId"));
		
		// Loop over all sections and set the leaf property to 
		// signify it being in use by the current company.
		for (Node n : hierarchy) {
			if (activeNodes.contains(n.getNodeId())) {
				n.setLeaf(true);
			} else {
				n.setLeaf(false);
			}
		}
		super.putModuleData(hierarchy);
	}


	/**
	 * Gets all sections that have been assigned to the supplied company
	 * @param companyId
	 * @return
	 * @throws ActionException
	 */
	private List<String> getActiveSections(String productId) throws ActionException {
		StringBuilder sql = new StringBuilder(150);
		sql.append("SELECT SECTION_ID FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_PRODUCT_SECTION WHERE PRODUCT_ID = ? ");
		
		List<String> activeSections = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, productId);
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				activeSections.add(rs.getString("SECTION_ID"));
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
		
		return activeSections;
		
	}
	
	
	/**
	 * Get all attributes associated with the supplied product.
	 * @param product
	 */
	private void addAttributes(ProductVO product) {
		List<Object> results = getProductAttributes(product.getProductId());
		for (Object o : results) {
			product.addAttribute((ProductAttributeVO)o);
		}
	}
	
	
	/**
	 * Returns a list of attributes for a product
	 * @param productId
	 * @return
	 */
	private List<Object> getProductAttributes(String productId) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_PRODUCT_ATTRIBUTE_XR ");
		sql.append("WHERE PRODUCT_ID = ? ");
		log.debug(sql+"|"+productId);
		List<Object> params = new ArrayList<>();
		params.add(productId);
		DBProcessor db = new DBProcessor(dbConn);
		
		// DBProcessor returns a list of objects that need to be individually cast to attributes
		return db.executeSelect(sql.toString(), params, new ProductAttributeVO());
	}

	
	/**
	 * Update a product or related attribute of a product
	 * @param req
	 * @throws ActionException
	 */
	private void updateElement(ActionRequest req) throws ActionException {
		ActionTarget action = ActionTarget.valueOf(req.getParameter(ACTION_TARGET));
		DBProcessor db = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
		switch(action) {
			case PRODUCT:
				ProductVO c = new ProductVO(req);
				saveProduct(c, db);
				break;
			case PRODUCTATTRIBUTE:
				ProductAttributeVO attr = new ProductAttributeVO(req);
				saveAttribute(attr, db);
				break;
			case ATTRIBUTE:
				ProductAttributeTypeVO t = new ProductAttributeTypeVO(req);
				saveAttributeType(t, db, Convert.formatBoolean(req.getParameter("insert")));
				break;
			case SECTION:
				saveSections(req);
				break;
			default:;
		}
	}


	/**
	 * Add the supplied sections to the company xr table
	 * @param req
	 * @throws ActionException
	 */
	private void saveSections(ActionRequest req) throws ActionException {
		// Delete all sections currently assigned to this company before adding
		// what is on the request object.
		deleteSection(true, req.getParameter("companyId"));
		
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
	private void saveAttribute(ProductAttributeVO attr, DBProcessor db) throws ActionException {
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
	private void saveProduct(ProductVO c, DBProcessor db) throws ActionException {
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
	private void saveAttributeType(ProductAttributeTypeVO t, DBProcessor db, Boolean insert) throws ActionException {
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
	private void deleteElement(ActionRequest req) throws ActionException {
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
			case SECTION:
				deleteSection(false, req.getParameter("sectionId"));
				break;
			default:;
		}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}
	
	
	/**
	 * Delete section xrs for a company. Deletes come in single xr deletion and
	 * full wipes used when new xrs are being saved.
	 * @param full
	 * @param id
	 * @throws ActionException
	 */
	private void deleteSection(boolean full, String id) throws ActionException {
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
		
		redirectRequest(msg, buildAction, req);
	}


	/**
	 * Build the redirect for build requests
	 * @param msg
	 * @param buildAction
	 * @param req
	 */
	private void redirectRequest(String msg, String buildAction, ActionRequest req) {
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
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}
}
