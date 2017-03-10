package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.biomed.smarttrak.admin.AbstractTreeAction;
import com.biomed.smarttrak.security.SecurityController;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.AllianceVO;
import com.biomed.smarttrak.vo.CompanyAttributeVO;
import com.biomed.smarttrak.vo.CompanyVO;
import com.biomed.smarttrak.vo.LocationVO;
import com.biomed.smarttrak.vo.ProductVO;
import com.biomed.smarttrak.vo.SectionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.solr.SecureSolrDocumentVO.Permission;

/****************************************************************************
 * <b>Title</b>: CompanyAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Return either a list of companies by search terms
 * or details on a particular company.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Feb 15, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class CompanyAction extends AbstractTreeAction {
	
	private static final String DEFAULT_GROUP = "Other";
	
	public CompanyAction() {
		super();
	}

	public CompanyAction(ActionInitVO init) {
		super(init);
	}
	
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (req.hasParameter("reqParam_1")) {
			CompanyVO vo = retrieveCompany(req.getParameter("reqParam_1"));
			try {
				SecurityController.getInstance(req).isUserAuthorized(vo, req);
			} catch(Exception e) {
				PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
				sbUtil.manualRedirect(req,page.getFullPath());
				return;
			}
			putModuleData(vo);
		} else if (req.hasParameter("searchData") || req.hasParameter("fq") || req.hasParameter("hierarchyList")){
			retrieveCompanies(req);
		}
	}

	
	/**
	 * Get the company for the supplied id
	 * @param companyId
	 * @throws ActionException
	 */
	protected CompanyVO retrieveCompany(String companyId) throws ActionException {
		StringBuilder sql = new StringBuilder(275);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT c.*, parent.COMPANY_NM as PARENT_NM  FROM ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY parent ");
		sql.append("ON c.PARENT_ID = parent.COMPANY_ID ");
		sql.append("WHERE c.COMPANY_ID = ? ");

		DBProcessor db = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
		List<Object> params = new ArrayList<>();
		params.add(companyId);
		CompanyVO company;
		try {
			List<Object> results = db.executeSelect(sql.toString(), params, new CompanyVO());
			if (results.isEmpty()) throw new ActionException("No company found with id " + companyId);
			
			company = (CompanyVO) results.get(0);
			addAttributes(company);
			addLocations(company);
			addProducts(company);
			addSections(company);
			addAlliances(company);
			addInvestors(company);
		} catch (Exception e) {
			throw new ActionException(e);
		}
		return company;
	}
	
	/**
	 * Returns data from the company record only.
	 * 
	 * @param companyId
	 * @return
	 * @throws ActionException 
	 */
	public CompanyVO getCompany(String companyId) throws ActionException {
		DBProcessor dbp = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
		
		CompanyVO company = new CompanyVO();
		company.setCompanyId(companyId);
		
		try {
			dbp.getByPrimaryKey(company);
		} catch (Exception e) {
			throw new ActionException("Couldn't retrieve company record.", e);
		}
		
		return company;
	}

	/**
	 * Get all companies that have invested in the supplied company and add
	 * them to the vo.
	 * @param company
	 */
	protected void addInvestors(CompanyVO company) throws ActionException {
		StringBuilder sql = new StringBuilder(175);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT i.INVESTOR_COMPANY_ID, c.COMPANY_NM FROM ").append(customDb).append("BIOMEDGPS_COMPANY_INVESTOR i ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("ON c.COMPANY_ID = i.INVESTOR_COMPANY_ID ");
		sql.append("WHERE i.INVESTEE_COMPANY_ID = ? ");
		log.debug(sql+"|"+company.getCompanyId());
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, company.getCompanyId());
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				company.addInvestor(rs.getString("INVESTOR_COMPANY_ID"), rs.getString("COMPANY_NM"));
			}
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		
	}

	
	/**
	 * Get all products associated with the supplied company
	 * @param company
	 * @throws ActionException
	 */
	private void addProducts(CompanyVO company) throws ActionException {
		StringBuilder sql = new StringBuilder(400);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		sql.append("SELECT p.PRODUCT_NM, p.PRODUCT_ID, xr.ATTRIBUTE_ID FROM ").append(customDb).append("BIOMEDGPS_PRODUCT p ");
		sql.append("INNER JOIN ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("ON c.COMPANY_ID = p.COMPANY_ID and c.COMPANY_ID = ? ");
		sql.append("INNER JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE_XR xr ");
		sql.append("on p.PRODUCT_ID = xr.PRODUCT_ID and xr.PRODUCT_ATTRIBUTE_ID = ( SELECT PRODUCT_ATTRIBUTE_ID ");
		sql.append("FROM ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE_XR x ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT_ATTRIBUTE a ");
		sql.append("ON a.ATTRIBUTE_ID = x.ATTRIBUTE_ID ");
		sql.append("WHERE x.PRODUCT_ID = p.PRODUCT_ID and a.TYPE_CD = 'HTML' limit 1 ) ");
		log.debug(sql+"|"+company.getCompanyId());
		
		Tree t = buildAttributeTree("PRODUCT");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, company.getCompanyId());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				
				ProductVO p = new ProductVO();
				p.setProductId(rs.getString("PRODUCT_ID"));
				p.setProductName(rs.getString("PRODUCT_NM"));
				addToProductMap(company, t, p, rs.getString("ATTRIBUTE_ID"));
			}
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}
	
	
	
	/**
	 * Group the products according to thier groups.
	 * @param company
	 * @param attributeTree
	 * @param prod
	 * @param attrId
	 */
	private void addToProductMap(CompanyVO company, Tree attributeTree, ProductVO prod, String attrId) {
		String[] path = attributeTree.findNode(attrId).getFullPath().split("/");
		
		// Markets using attributes too high up in the tree do not have enough
		// information to be sorted properly and are placed in the extras group.
		if (path.length < 2) {
			company.addProduct(DEFAULT_GROUP, prod);
			return;
		}
		
		Node n = attributeTree.findNode(path[1]);

		company.addProduct(n.getNodeName(), prod);
	}
	
	
	/**
	 * Get all alliances the supplied company is in and add them to the vo
	 * @param company
	 */
	protected void addAlliances(CompanyVO company) {
		StringBuilder sql = new StringBuilder(400);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_COMPANY_ALLIANCE_XR cax ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_ALLIANCE_TYPE at ");
		sql.append("ON cax.ALLIANCE_TYPE_ID = at.ALLIANCE_TYPE_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("ON c.COMPANY_ID = cax.REL_COMPANY_ID ");
		sql.append("WHERE cax.COMPANY_ID = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(company.getCompanyId());
		DBProcessor db = new DBProcessor(dbConn);
		
		// DBProcessor returns a list of objects that need to be individually cast to alliances
		List<Object> results = db.executeSelect(sql.toString(), params, new AllianceVO());
		for (Object o : results) {
			company.addAlliance((AllianceVO)o);
		}
	}

	
	/**
	 * Get all locations supported by the supplied company and add them to the vo.
	 * @param company
	 */
	protected void addLocations(CompanyVO company) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_COMPANY_LOCATION ");
		sql.append("WHERE COMPANY_ID = ? ");
		log.debug(sql+"|"+company.getCompanyId());
		List<Object> params = new ArrayList<>();
		params.add(company.getCompanyId());
		DBProcessor db = new DBProcessor(dbConn);
		
		// DBProcessor returns a list of objects that need to be individually cast to locations
		List<Object> results = db.executeSelect(sql.toString(), params, new LocationVO());
		for (Object o : results) {
			company.addLocation((LocationVO)o);
		}
	}
	
	
	/**
	 * Add all attributes to the supplied company
	 * @param company
	 * @throws ActionException
	 */
	protected void addAttributes(CompanyVO company) throws ActionException {
		StringBuilder sql = new StringBuilder(150);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_COMPANY_ATTRIBUTE_XR xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY_ATTRIBUTE a ");
		sql.append("ON a.ATTRIBUTE_ID = xr.ATTRIBUTE_ID ");
		sql.append("WHERE COMPANY_ID = ? ");
		sql.append("ORDER BY ORDER_NO ");
		log.debug(sql+"|"+company.getCompanyId());
		
		List<Object> params = new ArrayList<>();
		params.add(company.getCompanyId());
		DBProcessor db = new DBProcessor(dbConn);
		
		List<Object> results = db.executeSelect(sql.toString(), params, new CompanyAttributeVO());
		Tree t = buildAttributeTree("COMPANY");
		Map<String, List<CompanyAttributeVO>> attrMap = new TreeMap<>();
		for (Object o : results) {
			addToAttributeMap(attrMap, t, (CompanyAttributeVO)o);
		}
		
		for (Entry<String, List<CompanyAttributeVO>> e : attrMap.entrySet()) {
			for (CompanyAttributeVO attr : e.getValue()) {
				company.addCompanyAttribute(attr);
			}
		}
	}

	
	/**
	 * Get all sections for the supplied company
	 * @param company
	 * @throws ActionException
	 */
	protected void addSections(CompanyVO company) throws ActionException {
		StringBuilder sql = new StringBuilder(350);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY_SECTION cs ");
		sql.append("ON c.COMPANY_ID = cs.COMPANY_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_SECTION s ");
		sql.append("ON s.SECTION_ID = cs.SECTION_ID ");
		sql.append("WHERE c.COMPANY_ID = ? ");
		log.debug(sql);

		SmarttrakTree t = loadDefaultTree();
		t.buildNodePaths();
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, company.getCompanyId());
			
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				company.addCompanySection(new GenericVO(rs.getString("COMPANY_SECTION_XR_ID"), rs.getString("SECTION_NM")));
				Node n = t.findNode(rs.getString("SECTION_ID"));
				if (n != null) {
					SectionVO sec = (SectionVO) n.getUserObject();
					company.addACLGroup(Permission.GRANT, sec.getSolrTokenTxt());
				}
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}

	
	/**
	 * Get all companies from solr
	 * @param req
	 * @throws ActionException
	 */
	protected void retrieveCompanies(ActionRequest req) throws ActionException {
		// Pass along the proper information for a search to be done.
	    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
	    	actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
	    	req.setParameter("pmid", mod.getPageModuleId());
		
	    	// Build the solr action
		SolrAction sa = new SolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
		sa.retrieve(req);
	}


	/**
	 * Group the attributes by thier ancestry
	 * @param attrMap
	 * @param attributeTree
	 * @param attr
	 */
	private void addToAttributeMap(Map<String, List<CompanyAttributeVO>> attrMap, Tree attributeTree, CompanyAttributeVO attr) {
		String[] path = attributeTree.findNode(attr.getAttributeId()).getFullPath().split("/");
		
		// Markets using attributes too high up in the tree do not have enough
		// information to be sorted properly and are placed in the extras group.
		if (path.length < 2) {
			attr.setGroupName(DEFAULT_GROUP);
			attrMap.get(DEFAULT_GROUP).add(attr);
			return;
		}
		
		Node n = attributeTree.findNode(path[1]);
		
		if (!attrMap.keySet().contains(n.getNodeName())) {
			attrMap.put(n.getNodeName(), new ArrayList<CompanyAttributeVO>());
		}

		attr.setGroupName(n.getNodeName());
		attrMap.get(n.getNodeName()).add(attr);
	}
	

	/**
	 * Create the full attribute tree in order to determine the full ancestry of each attribute
	 * @param attrType
	 * @return
	 * @throws ActionException
	 */
	private Tree buildAttributeTree(String attrType) throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT c.ATTRIBUTE_ID, c.PARENT_ID, c.ATTRIBUTE_NM, p.ATTRIBUTE_NM as PARENT_NM, ");
		if ("PRODUCT".equals(attrType)) {
			sql.append("c.ORDER_NO ");
		} else {
			sql.append("c.DISPLAY_ORDER_NO ");
		}
		sql.append("FROM ").append(customDb).append("BIOMEDGPS_").append(attrType).append("_ATTRIBUTE c ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_").append(attrType).append("_ATTRIBUTE p ");
		sql.append("ON c.PARENT_ID = p.ATTRIBUTE_ID ");
		log.debug(sql);
		List<Node> attributes = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Node n = new Node(rs.getString("ATTRIBUTE_ID"), rs.getString("PARENT_ID"));
				setNodeName(n, rs, attrType);
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
	 * Set the name of the supplied node based on the passed attribute type.
	 */
	protected void setNodeName(Node n, ResultSet rs, String attrType) throws SQLException {
		if ("PRODUCT".equals(attrType)) {
			n.setNodeName(rs.getInt("ORDER_NO") + "|" + rs.getString("ATTRIBUTE_NM"));
		} else {
			if ("profile".equals(rs.getString("ATTRIBUTE_NM"))) {
				n.setNodeName(rs.getInt("DISPLAY_ORDER_NO") + "|" + rs.getString("PARENT_NM"));
			} else {
				n.setNodeName(rs.getInt("DISPLAY_ORDER_NO") + "|" + rs.getString("ATTRIBUTE_NM"));
			}
		}
	}

	@Override
	public String getCacheKey() {
		return null;
	}
}
