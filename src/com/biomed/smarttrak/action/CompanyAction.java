package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
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
	private static final int PRODUCT_PATH_LENGTH = 2;
	
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
			if (StringUtil.isEmpty(vo.getCompanyId())){
				PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
				sbUtil.manualRedirect(req,page.getFullPath());
			} else {
				SecurityController.getInstance(req).isUserAuthorized(vo, req);
			    	PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
			    	SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
				page.setTitleName(vo.getCompanyName() + " | " + site.getSiteName());
				putModuleData(vo);
			}
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
			if (results.isEmpty()) return new CompanyVO();
			
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
		
		sql.append("SELECT p.PRODUCT_NM, p.PRODUCT_ID, s.SECTION_ID FROM ").append(customDb).append("BIOMEDGPS_PRODUCT p ");
		sql.append("INNER JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT_SECTION s ");
		sql.append("on p.PRODUCT_ID = s.PRODUCT_ID ");
		sql.append("WHERE p.COMPANY_ID = ? ");
		log.debug(sql+"|"+company.getCompanyId());

		SmarttrakTree t = loadDefaultTree();
		t.buildNodePaths();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, company.getCompanyId());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				
				ProductVO p = new ProductVO();
				p.setProductId(rs.getString("PRODUCT_ID"));
				p.setProductName(rs.getString("PRODUCT_NM"));
				addToProductMap(company, t, p, rs.getString("SECTION_ID"));
			}
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}
	
	
	
	/**
	 * Group the products according to thier groups.
	 * @param company
	 * @param sectionTree
	 * @param prod
	 * @param sectionId
	 */
	private void addToProductMap(CompanyVO company, Tree sectionTree, ProductVO prod, String sectionId) {
		String[] path = sectionTree.findNode(sectionId).getFullPath().split(SearchDocumentHandler.HIERARCHY_DELIMITER);
		
		// Markets using attributes too high up in the tree do not have enough
		// information to be sorted properly and are placed in the extras group.
		if (path.length < PRODUCT_PATH_LENGTH) {
			company.addProduct(path[path.length-1], prod);
			return;
		}
		
		company.addProduct(path[PRODUCT_PATH_LENGTH-1], prod);
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
	 * Get all locations supported by the supplied company, its children, and its grandchildren and add them to the vo.
	 * @param company
	 */
	protected void addLocations(CompanyVO company) {
		StringBuilder sql = new StringBuilder(650);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT l.* FROM ").append(customDb).append("BIOMEDGPS_COMPANY_LOCATION l ");
		sql.append("left join ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("on c.COMPANY_ID = l.COMPANY_ID ");
		sql.append("WHERE l.COMPANY_ID = ? or c.PARENT_ID = ? or c.PARENT_ID in (");
		sql.append("SELECT child.COMPANY_ID FROM ").append(customDb).append("BIOMEDGPS_COMPANY parent ");
		sql.append("left join ").append(customDb).append("BIOMEDGPS_COMPANY child ");
		sql.append("on parent.COMPANY_ID = child.PARENT_ID ");
		sql.append("WHERE parent.COMPANY_ID = ? ) ");
		sql.append("order by c.PARENT_ID desc, PRIMARY_LOCN_FLG asc ");
		log.debug(sql+"|"+company.getCompanyId());
		List<Object> params = new ArrayList<>();
		params.add(company.getCompanyId());
		params.add(company.getCompanyId());
		params.add(company.getCompanyId());
		DBProcessor db = new DBProcessor(dbConn);
		
		// DBProcessor returns a list of objects that need to be individually cast to locations
		List<Object> results = db.executeSelect(sql.toString(), params, new LocationVO());
		for (Object o : results) {
			log.debug("Adding  " + ((LocationVO)o).getLocationId());
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
		sql.append("SELECT xr.*, a.* FROM ").append(customDb).append("BIOMEDGPS_COMPANY_ATTRIBUTE_XR xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY_ATTRIBUTE a ");
		sql.append("ON a.ATTRIBUTE_ID = xr.ATTRIBUTE_ID ");
		sql.append("WHERE COMPANY_ID = ? ");
		sql.append("ORDER BY a.DISPLAY_ORDER_NO, xr.ORDER_NO ");
		log.debug(sql+"|"+company.getCompanyId());
		
		List<Object> params = new ArrayList<>();
		params.add(company.getCompanyId());
		DBProcessor db = new DBProcessor(dbConn);
		
		List<Object> results = db.executeSelect(sql.toString(), params, new CompanyAttributeVO());
		Map<String, List<CompanyAttributeVO>> attrMap = new LinkedHashMap<>();
		for (Object o : results) {
			addToAttributeMap(attrMap, (CompanyAttributeVO)o);
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
				company.addCompanySection(new SectionVO(rs));
				Node n = null;
				
				if (!StringUtil.isEmpty(rs.getString("SECTION_ID"))) 
					n = t.findNode(rs.getString("SECTION_ID"));
				
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
	private void addToAttributeMap(Map<String, List<CompanyAttributeVO>> attrMap, CompanyAttributeVO attr) {
		
		if ("LINK".equals(attr.getAttributeTypeName()) ||
				"ATTACH".equals(attr.getAttributeTypeName())) {
			addLink(attrMap, attr);
			return;
		}
		
		String name = attr.getAttributeName();
		
		if (!attrMap.keySet().contains(name)) {
			attrMap.put(name, new ArrayList<CompanyAttributeVO>());
		}

		attr.setGroupName(name);
		attrMap.get(name).add(attr);
	}
	

	/**
	 * Add the link to the proper list, including specialized lists for attatchments
	 * @param attrMap
	 * @param attr
	 */
	private void addLink(Map<String, List<CompanyAttributeVO>> attrMap,
			CompanyAttributeVO attr) {
		if (attrMap.get(attr.getAttributeId()) == null) attrMap.put(attr.getAttributeId(), new ArrayList<CompanyAttributeVO>());
		attrMap.get(attr.getAttributeId()).add(attr);
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
