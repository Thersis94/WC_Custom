package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.solr.common.SolrDocument;

import com.biomed.smarttrak.vo.ProductAllianceVO;
import com.biomed.smarttrak.vo.ProductAttributeVO;
import com.biomed.smarttrak.vo.ProductVO;
import com.biomed.smarttrak.vo.RegulationVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrFieldVO;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.action.search.SolrFieldVO.BooleanType;
import com.smt.sitebuilder.action.search.SolrFieldVO.FieldType;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>: ProductAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Return either a list of products by search terms
 * or details on a particular product.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Feb 15, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class ProductAction extends SBActionAdapter {
	public static final String DETAILS_ID = "DETAILS_ROOT";
	
	public ProductAction() {
		super();
	}

	public ProductAction(ActionInitVO init) {
		super(init);
	}
	
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (req.hasParameter("reqParam_1")) {
			retrieveProduct(req.getParameter("reqParam_1"));
		} else if (req.hasParameter("searchData") || req.hasParameter("selNodes")){
			retrieveProducts(req);
		}
	}

	protected void retrieveProduct(String productId) throws ActionException {
		ProductVO product;
		StringBuilder sql = new StringBuilder(100);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_PRODUCT p ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("ON c.COMPANY_ID = p.COMPANY_ID ");
		sql.append("WHERE PRODUCT_ID = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(productId);
		DBProcessor db = new DBProcessor(dbConn);
		product = (ProductVO) db.executeSelect(sql.toString(), params, new ProductVO()).get(0);

		// Get specifics on product details
		addAttributes(product);
		addSections(product);
		addAlliances(product);
		addRegulatory(product);
		// Related products are based on company, no id no related companies.
		if (!StringUtil.isEmpty(product.getCompanyId()))
			addRelatedProducts(product);
		
		super.putModuleData(product);
	}

	/**
	 * Add all products from the owning company to the vo
	 */
	protected void addRelatedProducts(ProductVO product) throws ActionException {
		StringBuilder sql = new StringBuilder(375);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT p.PRODUCT_ID, p.PRODUCT_NM, s.SECTION_NM FROM ");
		sql.append(customDb).append("BIOMEDGPS_PRODUCT p ");
		sql.append("INNER JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT_SECTION xr ");
		sql.append("ON xr.PRODUCT_ID = p.PRODUCT_ID ");
		sql.append("INNER JOIN ").append(customDb).append("BIOMEDGPS_SECTION s ");
		sql.append("ON xr.SECTION_ID = s.SECTION_ID ");
		sql.append("WHERE p.COMPANY_ID = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, product.getCompanyId());
			
			ResultSet rs = ps.executeQuery();

			DBProcessor db = new DBProcessor(dbConn);
			while(rs.next()) {
				ProductVO p = new ProductVO();
				db.executePopulate(p, rs);
				product.addRelatedProduct(rs.getString("SECTION_NM"), p);
			}
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}

	/**
	 * Add all regulations to the product
	 */
	protected void addRegulatory(ProductVO product) {
		StringBuilder sql = new StringBuilder(475);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_PRODUCT_REGULATORY r ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_REGULATORY_STATUS s ");
		sql.append("ON s.STATUS_ID = r.STATUS_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_REGULATORY_REGION re ");
		sql.append("ON re.REGION_ID = r.REGION_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_REGULATORY_PATH p ");
		sql.append("ON p.PATH_ID = r.PATH_ID ");
		sql.append("WHERE r.PRODUCT_ID = ? ");
		
		DBProcessor db = new DBProcessor(dbConn);
		List<Object> params = new ArrayList<>();
		params.add(product.getProductId());
		List<Object> results = db.executeSelect(sql.toString(), params, new RegulationVO());
		for (Object o : results) {
			product.addRegulation((RegulationVO) o);
		}
	}
	
	
	/**
	 * Get all attributes for a product
	 * @param product
	 * @throws ActionException
	 */
	protected void addAttributes(ProductVO product) throws ActionException {
		StringBuilder sql = new StringBuilder(150);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE_XR xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE a ");
		sql.append("ON a.ATTRIBUTE_ID = xr.ATTRIBUTE_ID ");
		sql.append("WHERE PRODUCT_ID = ? ");
		sql.append("ORDER BY a.ORDER_NO, xr.ORDER_NO ");
		log.debug(sql+"|"+product.getProductId());
		
		List<Object> params = new ArrayList<>();
		params.add(product.getProductId());
		DBProcessor db = new DBProcessor(dbConn);
		
		List<Object> results = db.executeSelect(sql.toString(), params, new ProductAttributeVO());
		Tree t = buildAttributeTree();
		Map<String, List<ProductAttributeVO>> attrMap = new TreeMap<>();
		for (Object o : results) {
			addToAttributeMap(attrMap, t, (ProductAttributeVO)o, product);
		}
		
		for (Entry<String, List<ProductAttributeVO>> e : attrMap.entrySet()) {
			for (ProductAttributeVO attr : e.getValue()) {
				product.addAttribute(attr);
			}
		}
		
	}

	
	/**
	 * Build an attribute tree to get a complete listing of the attribute groups
	 * @return
	 * @throws ActionException
	 */
	protected Tree buildAttributeTree() throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("SELECT ATTRIBUTE_ID, PARENT_ID, ATTRIBUTE_NM, ORDER_NO ");
		sql.append("FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_PRODUCT_ATTRIBUTE ");
		log.debug(sql);
		List<Node> attributes = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Node n = new Node(rs.getString("ATTRIBUTE_ID"), rs.getString("PARENT_ID"));
				n.setNodeName(rs.getInt("ORDER_NO") + "|" + rs.getString("ATTRIBUTE_NM"));
				attributes.add(n);
			}
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		Tree t = new Tree(attributes);
		t.buildNodePaths();
		return t;
	}
	
	
	/**
	 * Add an attribute to map properly groups attributes according to ancestry.
	 * @param attrMap
	 * @param attributeTree
	 * @param attr
	 * @param product
	 */
	protected void addToAttributeMap(Map<String, List<ProductAttributeVO>> attrMap, Tree attributeTree, ProductAttributeVO attr, ProductVO product) {
		Node n = attributeTree.findNode(attr.getAttributeId());
		
		String[] path = n.getFullPath().split("/");
		
		// Markets using attributes too high up in the tree do not have enough
		// information to be sorted properly and are placed in the extras group.
		if (path.length < 2) {
			attr.setGroupName("Other");
			attrMap.get("Other").add(attr);
			return;
		}
		Node head = attributeTree.findNode(path[1]);
		if (n.getFullPath().contains(DETAILS_ID)) {
			String[] name = head.getNodeName().split("\\|");
			product.addDetail(name[name.length-1], attr);
		} else {
			if (!attrMap.keySet().contains(path[1])) {
				attrMap.put(path[1], new ArrayList<ProductAttributeVO>());
			}

			attr.setGroupName(head.getNodeName());
			attrMap.get(path[1]).add(attr);
		}
		
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
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE_XR ");
		sql.append("WHERE PRODUCT_ID = ? AND ATTRIBUTE_ID not in ( ");
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
	 * Get all the sections that are associated with the supplied product
	 * @param product
	 * @throws ActionException
	 */
	protected void addSections(ProductVO product) throws ActionException {
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
	 * Retrieve all products from solr according to the search terms
	 * @param req
	 * @throws ActionException
	 */
	protected void retrieveProducts(ActionRequest req) throws ActionException {
		SolrActionVO qData = buildSolrAction(req);
		
		if (req.hasParameter("searchData")) 
			qData.setSearchData("*"+req.getParameter("searchData")+"*");
		
		StringBuilder selected = new StringBuilder(50);
		if (req.hasParameter("selNodes")) {
			selected.append("(");
			for (String s : req.getParameterValues("selNodes")) {
				if (selected.length() > 2) selected.append(" OR ");
				selected.append("*").append(s);
			}
			selected.append(")");
			qData.addSolrField(new SolrFieldVO(FieldType.FILTER, SearchDocumentHandler.SECTION, selected.toString(), BooleanType.AND));
		}
		
		SolrQueryProcessor sqp = new SolrQueryProcessor(attributes, qData.getSolrCollectionPath());
		SolrResponseVO vo = sqp.processQuery(qData);
		for (SolrDocument doc : vo.getResultDocuments()) {
			doc.setField("updateMsg", buildUpdateMsg(doc));
		}
		
		super.putModuleData(vo);
	}
	

	/**
	 * Build the time since update message for this product
	 * @param doc
	 * @return
	 */
	protected String buildUpdateMsg(SolrDocument doc) {
		// Unpublished companies can be skipped
		if (!"P".equals(doc.get(SearchDocumentHandler.CONTENT_TYPE))) {
			return "Unpublished";
		}
		Date d = (Date) doc.get(SearchDocumentHandler.UPDATE_DATE);
		long diff = Convert.getCurrentTimestamp().getTime() -d.getTime();
		long diffDays = diff / (1000 * 60 * 60 * 24);
		long diffHours = diff / (1000 * 60 * 60);
		if (diffDays > 365) {
			int years = (int) (diffDays/365);
			return years + " year(s) ago";
		} else if (diffDays > 30) {
			int months = (int) (diffDays/30);
			return months + " month(s) ago";
		} else if (diffDays > 7) {
			int weeks = (int) (diffDays/7);
			return weeks + " week(s) ago";
		} else if (diffDays > 0) {
			return diffDays + " day(s) ago";
		} else if (diffHours > 0) {
			return diffHours + " hour(s) ago";
		} else {
			return "less than an hour ago";
		}
	}
	
	
	/**
	 * Get the solr information 
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected SolrActionVO buildSolrAction(ActionRequest req) throws ActionException {
		SolrAction sa = new SolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
	    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
	    	actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		return sa.retrieveActionData(req);
	}

}