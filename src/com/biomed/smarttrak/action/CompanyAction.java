package com.biomed.smarttrak.action;

// Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.biomed.smarttrak.action.AdminControllerAction.LinkType;
import com.biomed.smarttrak.action.AdminControllerAction.Status;
// WC Custom
import com.biomed.smarttrak.admin.SectionHierarchyAction;
import com.biomed.smarttrak.security.SecurityController;
import com.biomed.smarttrak.security.SmarttrakRoleVO;
import com.biomed.smarttrak.util.BiomedLinkCheckerUtil;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.AllianceVO;
import com.biomed.smarttrak.vo.CompanyAttributeVO;
import com.biomed.smarttrak.vo.CompanyVO;
import com.biomed.smarttrak.vo.LocationVO;
import com.biomed.smarttrak.vo.ProductVO;
import com.biomed.smarttrak.vo.SectionVO;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.solr.AccessControlQuery;
// WC Core
import com.smt.sitebuilder.action.SimpleActionAdapter;
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
public class CompanyAction extends SimpleActionAdapter {
	private static final int PRODUCT_PATH_LENGTH = 2;

	public CompanyAction() {
		super();
	}

	public CompanyAction(ActionInitVO init) {
		super(init);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (req.hasParameter("reqParam_1")) {
			SmarttrakRoleVO role = (SmarttrakRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);
			if (role == null)
				SecurityController.throwAndRedirect(req);
			CompanyVO vo = retrieveCompany(req.getParameter("reqParam_1"), role, false);

			// If a company has 0 products it should not be shown or no companyId, it shouldn't be shown.
			if (StringUtil.isEmpty(vo.getCompanyId())) {
				PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
				sbUtil.manualRedirect(req,page.getFullPath());
			} else if(!"P".equals(vo.getStatusNo()) || vo.getProducts().isEmpty()) {
				req.setParameter("showError", "true");
			} else {
				SecurityController.getInstance(req).isUserAuthorized(vo, req);
				PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
				SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
				page.setTitleName(vo.getCompanyName() + " | " + site.getSiteName());
				putModuleData(vo);
			}
		} else if (req.hasParameter("amid")){
			retrieveCompanies(req);
		}
	}


	/**
	 * Get the company for the supplied id
	 * @param companyId
	 * @throws ActionException
	 */
	public CompanyVO retrieveCompany(String companyId, SmarttrakRoleVO role, boolean allowAll) throws ActionException {
		StringBuilder sql = new StringBuilder(275);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT c.*, parent.COMPANY_NM as PARENT_NM, d.SYMBOL_TXT ");
		sql.append("FROM ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY parent ");
		sql.append("ON c.PARENT_ID = parent.COMPANY_ID ");
		sql.append("LEFT JOIN CURRENCY d on d.CURRENCY_TYPE_ID = c.CURRENCY_TYPE_ID ");
		sql.append("WHERE c.COMPANY_ID = ? ");

		DBProcessor db = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
		List<Object> params = new ArrayList<>();
		params.add(companyId);
		CompanyVO company;
		try {
			List<Object> results = db.executeSelect(sql.toString(), params, new CompanyVO());
			if (results.isEmpty()) return new CompanyVO();

			company = (CompanyVO) results.get(0);

			if(!allowAll && !Status.P.name().equals(company.getStatusNo())) {
				return company;
			}
			addProducts(company, role.getRoleLevel());
			addAttributes(company, role);
			addLocations(company, role.getRoleLevel());
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
	private void addProducts(CompanyVO company, int roleLevel) throws ActionException {
		StringBuilder sql = new StringBuilder(600);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);

		sql.append("SELECT p.PRODUCT_NM, p.PRODUCT_ID, s.SECTION_ID, p.COMPANY_ID, c.COMPANY_NM, ");
		sql.append("SHORT_NM, SHORT_NM_TXT FROM ").append(customDb).append("BIOMEDGPS_PRODUCT p ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT_ALLIANCE_XR a ");
		sql.append("on p.PRODUCT_ID = a.PRODUCT_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("on c.COMPANY_ID = p.COMPANY_ID ");
		sql.append("INNER JOIN ").append(customDb).append("BIOMEDGPS_PRODUCT_SECTION s ");
		sql.append("on p.PRODUCT_ID = s.PRODUCT_ID ");
		sql.append("WHERE (p.COMPANY_ID = ? or a.COMPANY_ID = ?) and p.STATUS_NO in (");
		if (AdminControllerAction.STAFF_ROLE_LEVEL == roleLevel) {
			sql.append("'").append(AdminControllerAction.Status.E).append("', "); 
		}
		sql.append("'").append(AdminControllerAction.Status.P).append("') "); 
		sql.append("order by p.SHORT_NM ");
		log.debug(sql+"|"+company.getCompanyId());
		List<ProductVO> products = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, company.getCompanyId());
			ps.setString(2, company.getCompanyId());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {

				ProductVO p = new ProductVO();
				p.setProductId(rs.getString("PRODUCT_ID"));
				p.setProductName(rs.getString("PRODUCT_NM"));
				p.setShortName(rs.getString("SHORT_NM"));
				p.addSection(rs.getString("SECTION_ID"));
				p.setCompanyId(rs.getString("COMPANY_ID"));
				p.setCompanyName(rs.getString("COMPANY_NM"));
				p.setCompanyShortName(rs.getString("SHORT_NM_TXT"));
				products.add(p);
			}
		} catch (SQLException e) {
			throw new ActionException(e);
		}

		sortProducts(company, products);
	}


	/**
	 * Ensure that the product order matches that of the hierarchy
	 * @param company
	 * @param products
	 */
	private void sortProducts(CompanyVO company, List<ProductVO> products) {
		SmarttrakTree t = loadDefaultTree();
		t.buildNodePaths();
		List<Node> sorted = t.preorderList();

		for (Node n : sorted) {
			for (ProductVO p : products) {
				addToProductMap(company, n, p);
			}
		}

		// Ensure that the products are all in alphabetical order
		company.sortProducts();
	}


	/**
	 * Group the products according to their groups.
	 * @param company
	 * @param sectionTree
	 * @param prod
	 * @param sectionId
	 */
	private void addToProductMap(CompanyVO company, Node n, ProductVO prod) {
		// If the supplied node and the supplied product section do not match, return.
		if (prod.getSections().isEmpty() ||
				!n.getNodeId().equals(prod.getSections().get(0))) return;

		String[] path = n.getFullPath().split(SearchDocumentHandler.HIERARCHY_DELIMITER);

		// Products should never be this far up the hierachy tree
		// but we need to head of potential NPEs here.
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
		StringBuilder sql = new StringBuilder(650);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT cax.company_alliance_xr_id, cax.alliance_type_id, c.short_nm_txt, cax.reference_txt, c.company_nm, at.type_nm, ");
		sql.append("at.alliance_type_id, case when c.status_no = 'P' and COUNT(p.product_id) > 0 then cax.rel_company_id else '' end as rel_company_id ");
		sql.append("FROM ").append(customDb).append("BIOMEDGPS_COMPANY_ALLIANCE_XR cax ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_ALLIANCE_TYPE at ");
		sql.append("ON cax.ALLIANCE_TYPE_ID = at.ALLIANCE_TYPE_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("ON c.COMPANY_ID = cax.REL_COMPANY_ID ");
		sql.append("left join ").append(customDb).append("biomedgps_product p on p.company_id = c.company_id ");
		sql.append("WHERE cax.COMPANY_ID = ? ");
		sql.append("group by cax.company_alliance_xr_id, cax.alliance_type_id, c.short_nm_txt, cax.reference_txt, ");
		sql.append("cax.rel_company_id, c.company_nm, at.type_nm, at.alliance_type_id, c.status_no ");
		sql.append("ORDER BY at.TYPE_NM, c.COMPANY_NM ");
		log.debug(sql+"|"+company.getCompanyId());

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
	protected void addLocations(CompanyVO company, int roleLevel) {
		StringBuilder sql = new StringBuilder(650);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT l.* FROM ").append(customDb).append("BIOMEDGPS_COMPANY_LOCATION l ");
		sql.append("left join ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("on c.COMPANY_ID = l.COMPANY_ID ");
		sql.append("WHERE l.COMPANY_ID = ? and c.STATUS_NO in (");
		if (AdminControllerAction.STAFF_ROLE_LEVEL == roleLevel) {
			sql.append("'").append(AdminControllerAction.Status.E).append("', "); 
		}
		sql.append("'").append(AdminControllerAction.Status.P).append("') "); 
		sql.append("order by PRIMARY_LOCN_FLG desc ");
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
	protected void addAttributes(CompanyVO company, SmarttrakRoleVO role) {
		StringBuilder sql = new StringBuilder(1400);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append(DBUtil.SELECT_CLAUSE).append("xr.*, a.*, parent.ATTRIBUTE_NM as PARENT_NM ");
		sql.append(DBUtil.FROM_CLAUSE);
		sql.append(customDb).append("BIOMEDGPS_COMPANY_ATTRIBUTE_XR xr ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("BIOMEDGPS_COMPANY_ATTRIBUTE a ");
		sql.append("ON a.ATTRIBUTE_ID = xr.ATTRIBUTE_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("BIOMEDGPS_COMPANY_ATTRIBUTE parent ");
		sql.append("ON parent.ATTRIBUTE_ID = a.PARENT_ID ");
		sql.append(DBUtil.WHERE_CLAUSE).append(" COMPANY_ID = ? and STATUS_NO in (");
		if (AdminControllerAction.STAFF_ROLE_LEVEL == role.getRoleLevel()) {
			sql.append("'").append(AdminControllerAction.Status.E).append("', "); 
		}
		sql.append("'").append(AdminControllerAction.Status.P).append("') ");
		sql.append(DBUtil.ORDER_BY).append("a.DISPLAY_ORDER_NO, xr.ORDER_NO, XR.TITLE_TXT ");
		log.debug(sql+"|"+company.getCompanyId());

		List<Object> params = new ArrayList<>();
		params.add(company.getCompanyId());
		DBProcessor db = new DBProcessor(dbConn);

		List<Object> results = db.executeSelect(sql.toString(), params, new CompanyAttributeVO());
		filterAttributes(results, company, role);
		//adjust the links to manage links if in preview mode from manage tool
		if(Convert.formatBoolean(getAttribute(Constants.PAGE_PREVIEW))) adjustContentLinks(results); 
	}

	/**
	 * Filter supplied attributes based on thier sections and the user's acl 
	 */
	protected void filterAttributes(List<Object> results, CompanyVO company, SmarttrakRoleVO role) {
		String[] roleAcl = role.getAuthorizedSections(SmarttrakSolrAction.BROWSE_SECTION);
		Map<String, List<CompanyAttributeVO>> attrMap = new LinkedHashMap<>();
		SmarttrakTree t = loadDefaultTree();
		t.buildNodePaths();
		for (Object o : results) {
			CompanyAttributeVO attr = (CompanyAttributeVO)o;
			Node n = null;

			if (StringUtil.isEmpty(attr.getSectionId())) {
				if ("LINK".equals(attr.getAttributeTypeName()) || "ATTACH".equals(attr.getAttributeTypeName())) {
					attr.setGroupName(LinkType.getFromName(attr.getTitleText()).getIcon());
				}
				
				// Items that don't have sections are viewable by anyone.
				addToAttributeMap(attrMap, (CompanyAttributeVO)o);
			} else {
				n = t.findNode(attr.getSectionId());
			}

			// If n is null we can leave finish the loop as
			// the attribute is either already in the map now
			// or it is an anomaly that exists outside standard operations
			if (n == null) continue;

			SectionVO sec = (SectionVO) n.getUserObject();
			if (roleAcl == null || roleAcl.length == 0 || !AccessControlQuery.isAllowed("+g:" + sec.getSolrTokenTxt(), null, roleAcl)) {
				// Do nothing. This attribute cannot be seen by the current user
				// and there is nothing left for the loop to do
			} else {
				addToAttributeMap(attrMap, (CompanyAttributeVO)o);
				company.addACLGroup(Permission.GRANT, sec.getSolrTokenTxt());
			}
		}

		for (Entry<String, List<CompanyAttributeVO>> e : attrMap.entrySet()) {
			for (CompanyAttributeVO attr : e.getValue()) {
				company.addCompanyAttribute(attr);
			}
		}
	}

	/**
	 * Modifies public links to their corresponding manage tool link
	 * @param attributes
	 */
	protected void adjustContentLinks(List<Object> attributes) {
		SiteVO siteData = (SiteVO)getAttribute(Constants.SITE_DATA);
		BiomedLinkCheckerUtil linkUtil = new BiomedLinkCheckerUtil(dbConn, siteData);
		
		//update the links accordingly
		for (Object o : attributes) {
			CompanyAttributeVO attr = (CompanyAttributeVO)o;
			String attrTypeNm = attr.getAttributeTypeName();
			if("LINK".equals(attrTypeNm)) {
				attr.setValueText(linkUtil.modifyPlainURL(attr.getValueText()));
			}else if("HTML".equals(attrTypeNm)) {
				attr.setValueText(linkUtil.modifySiteLinks(attr.getValueText()));
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
	 * returns the default hierarchy built by the hierarchy action
	 * @return
	 */
	private SmarttrakTree loadDefaultTree() {
		// load the section hierarchy Tree from the hierarchy action
		SectionHierarchyAction sha = new SectionHierarchyAction();
		sha.setAttributes(getAttributes());
		sha.setDBConnection(getDBConnection());
		return sha.loadDefaultTree();
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
		if ("LINK".equals(attr.getAttributeTypeName()) || "ATTACH".equals(attr.getAttributeTypeName())) {
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
	private void addLink(Map<String, List<CompanyAttributeVO>> attrMap, CompanyAttributeVO attr) {
		//make sure the list we're about to append to exists on the map first
		if (attrMap.get(attr.getAttributeId()) == null) 
			attrMap.put(attr.getAttributeId(), new ArrayList<CompanyAttributeVO>());

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
}