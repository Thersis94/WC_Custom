package com.fastsigns.action.wizard;

// JDK 1.6.x
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


// FASTSIGNS Libs
import com.fastsigns.action.franchise.vo.FranchiseVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

// WC Libs
import com.smt.sitebuilder.action.dealer.DealerInfoAction;
import com.smt.sitebuilder.admin.action.OrganizationAction;
import com.smt.sitebuilder.admin.action.SitePageAction;
import com.smt.sitebuilder.admin.action.TemplateAction;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SiteWizardAction.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Feb 22, 2011<p/>
 * <b>Changes: Apr 17. 2012, Billy Larsen, Updated SiteWizardAction to be compatible with new site.</b>
 ****************************************************************************/
public class SiteWizardAction_GB extends SiteWizardAction {
 
	/**
	 * Localization Text Set
	 */
	public SiteWizardAction_GB() {
		FS_SITE_ID = "FTS_UK";
		FS_GROUP = "FAST_SIGNS_GB";

		posMsg1 = "You have successfully created the site: ";
		negMsg1 = "Unable to add new site: ";
		negMsg2 = ".  Please contact the system administrator for assistance.";
		negMsg3 = " because it already exists";
		negMsg4 = "Unable to add new site, Franchise ID or Franchise Location Id contained letters: ";
	}

	/**
	 * Localization Text Set
	 * @param actionInit
	 */
	public SiteWizardAction_GB(ActionInitVO actionInit) {
		super(actionInit);
		FS_SITE_ID = "FTS_UK";
		FS_GROUP = "FAST_SIGNS_UK";
		posMsg1 = "You have successfully created the site: ";
		negMsg1 = "Unable to add new site: ";
		negMsg2 = ".  Please contact the system administrator for assistance.";
		negMsg3 = " because it already exists";
		negMsg4 = "Unable to add new site, Franchise ID or Franchise Location Id contained letters: ";
	}
	
	/**
	 * Adds an entry to the FS franchise table.
	 * @param vo
	 * @throws SQLException
	 */
	public void addFranchiseEntry(FranchiseVO vo) throws SQLException {
		StringBuilder s = new StringBuilder();

		String customDbSchema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		s.append("insert into ").append(customDbSchema).append("fts_franchise ");
		s.append("(franchise_id, location_desc_option_id, primary_owner_nm, create_dt, ");
		s.append("right_image_id, center_image_url, country_cd) ");
		s.append("values(?, 13, ?, ?, 1, '/binary/org/FTS_GB/images/heading/FS_Default.jpg', ?) ");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, vo.getFranchiseId());
			ps.setString(2, vo.getOwnerName());
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setString(4, vo.getCountryCode());
			ps.executeUpdate();
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}
	
	/**
	 * 
	 * @param layoutId
	 * @param page
	 */
	public void addHomePage(String layoutId, String fId, SMTServletRequest req) 
	throws Exception {

		SMTActionInterface sai = new SitePageAction(this.actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		req.setParameter("templateId", layoutId);
		req.setParameter("startDate", Convert.formatDate(new Date(), Convert.DATE_SLASH_PATTERN));
		req.setParameter("parentPath", "/");
		req.setParameter("organizationId", req.getParameter("organizationId"));
		req.setParameter("parentId", "");
		req.setParameter("siteId", req.getParameter("siteId"));
		req.setParameter("aliasName", "home");
		req.setParameter("displayName", "Home");
		req.setParameter("titleName", "Welcome to FASTSIGNS");
		req.setParameter("metaKeyword", "");
		req.setParameter("metaDesc", "");
		req.setParameter("visible", "0");
		req.setParameter("defaultPage", "1");
		req.setParameter("orderNumber", "1");
		req.setParameter("externalPageUrl", "");
		req.setParameter("roles", new String[] {"0", "10", "100", "0a00141332afab61abff2da59568338d"}, true);
		req.setParameter("skipApproval", "true");
		req.setParameter("pendingSyncFlag", "0");
		sai.update(req);
	}
	
	/**
	 * 
	 * @param siteId
	 */
	public void assignTheme(FranchiseVO vo) throws Exception {
		String siteId = FS_SITE_ID + "_" + vo.getFranchiseId() + "_1";
		
		String sql = "update site_theme_impl set theme_menu_id = 'c0a8022318ef45c7af24e2c4568bde4f',";
		sql += "theme_stylesheet_id = 'c0a8022dc850e47a671bc997b13424f3' ";
		sql += "where site_id = ?";
		log.debug("Theme Update: " + sql + "|" + siteId);
		
		PreparedStatement ps = dbConn.prepareStatement(sql);
		ps.setString(1, siteId);
		ps.executeUpdate();
	}
	
	/**
	 * 
	 */
	public void assignTypes() {
		
		// Default Display
		defDisplay.add(makePageModule(true, true, true, null,"c0a8016564a7940e9195bd84416afd93", null, 1, 2));														// 3-Button Group below menu 2012
		//defDisplay.add(makePageModule("0a00141332afb0976020f5cb8b3c98f7","c0a8022363bdd195bac696fd46636514", null, 2, 2));										// Center Page Slider 2012
		defDisplay.add(makePageModule(true, true, true, null,"c0a8016564a89155842b990af697746c", null, 2, 5));														// Modules 2012
		defDisplay.add(makePageModule(true, true, true, null,"c0a80165f0c0ccdaef974a12b5ee3faa", null, 3, 7));														// Right Rail 2012
		//defDisplay.add(makePageModule(true, false, false, "0a00141332afb0be6020f5cb7842bf78","c0a80a07614b3c24224dd3d77221237a", "HEADER_TOP_LEFT_CONTENT", 0, 1));	// World Link 2012
		defDisplay.add(makePageModule(true, false, false, "c0a80223d5a0e718bb741827283b9ef8","c0a80a07614b3c24224dd3d77221237a", "HEADER_TOP_LEFT_CONTENT", 0, 1));	// World Link 2012 updated
		defDisplay.add(makePageModule(false, true, true, "0a00141332afb0c46020f5cb973253a9","c0a80a07614b3c24224dd3d77221237a", "HEADER_TOP_LEFT_CONTENT", 0, 1));	// World Link 2012 Logged In
		defDisplay.add(makePageModule(true, true, false, "FTS_CENTER_PAGE_"+centerId, "0a00141d8afabb8f1d07f6377fa000a6", "HEADER_RIGHT", 0, 1));		//Consultation portlet
		defDisplay.add(makePageModule(true, true, true, "0a00141332afb0b86020f5cb6ae7d9d3", "c0a802411c9e09843c052afd87f4bba1", "SITE_SEARCH", 1, 0));				// Site Search 2012
		defDisplay.add(makePageModule(true, true, true, null, "c0a802234b3c124378e7a6703fa3445", null, 1, 4));														// White Board

		// Secondary pages
		secDisplay.add(makePageModule(true, true, true, null,"c0a8016564a7940e9195bd84416afd93", null, 1, 2));														// 3-Button Group below menu 2012
		//secDisplay.add(makePageModule("7f0001016122294ce3852a7728cb4963","c0a8022363bdd195bac696fd46636514", null, 2, 4));										// Center Page Slider 2012
		secDisplay.add(makePageModule(true, true, true, null,"c0a80165f0c0ccdaef974a12b5ee3faa", null, 3, 6));														// Right Rail 2012
		//secDisplay.add(makePageModule(true, false, false, "0a00141332afb0be6020f5cb7842bf78","c0a80a07614b3c24224dd3d77221237a", "HEADER_TOP_LEFT_CONTENT", 0, 1));	// World Link 2012
		secDisplay.add(makePageModule(true, false, false, "c0a80223d5a0e718bb741827283b9ef8","c0a80a07614b3c24224dd3d77221237a", "HEADER_TOP_LEFT_CONTENT", 0, 1));	// World Link 2012 updated
		secDisplay.add(makePageModule(false, true, true, "0a00141332afb0c46020f5cb973253a9","c0a80a07614b3c24224dd3d77221237a", "HEADER_TOP_LEFT_CONTENT", 0, 1));	// World Link 2012 Logged In
		secDisplay.add(makePageModule(true, true, false, "FTS_CENTER_PAGE_"+centerId, "0a00141d8afabb8f1d07f6377fa000a6", "HEADER_RIGHT", 0, 1));		//Consultation Portlet
		secDisplay.add(makePageModule(true, true, true, "0a00141332afb0b86020f5cb6ae7d9d3", "c0a802411c9e09843c052afd87f4bba1", "SITE_SEARCH", 1, 0));				// Site Search 2012
		secDisplay.add(makePageModule(true, true, true, "45102F48BA5247C98BFBD00BBA9B8AEC", "c0a80a076e1dc62789a3565692e8803e", null, 2, 1));						// Breadcrumbs
		secDisplay.add(makePageModule(true, true, true, null, "c0a802234b3c124378e7a6703fa3445", null, 1, 4));														// White Board
		secDisplay.add(makePageModule(true,true, true, "FTS_CENTER_PAGE_"+centerId, "c0a80165f0c00ec5ab44b17238772bf9", null, 2, 4));								//Sub page Intro heading

		emptyColDisplay.add(makePageModule(true, false, false, "c0a80223d5a0e718bb741827283b9ef8","c0a80a07614b3c24224dd3d77221237a", "HEADER_TOP_LEFT_CONTENT", 0, 1));	// World Link 2012 updated
		emptyColDisplay.add(makePageModule(false, true, true, "0a00141332afb0c46020f5cb973253a9","c0a80a07614b3c24224dd3d77221237a", "HEADER_TOP_LEFT_CONTENT", 0, 1));		// World Link 2012 Logged In
		emptyColDisplay.add(makePageModule(true, true, true, "FTS_CENTER_PAGE_"+centerId, "0a00141d8afabb8f1d07f6377fa000a6", "HEADER_RIGHT", 0, 1));						//Consultation portlet
		emptyColDisplay.add(makePageModule(true, true, true, "0a00141332afb0b86020f5cb6ae7d9d3", "c0a802411c9e09843c052afd87f4bba1", "SITE_SEARCH", 1, 0));					// Site Search 2012
	}
	
	public Map<String, Integer> makeRoles(boolean isPublic, boolean isReg, boolean isAdmin) {
		Map<String, Integer> roles = new HashMap<String, Integer>();
		
		if (isPublic) roles.put("0", 0);
		
		if (isReg) {
			roles.put("10", 10);
			roles.put("0a00141332afab61abff2da59568338d", 30);  // the "Franchise" role
		}
		
		if (isAdmin) roles.put("100", 100);
		
		return roles;
	}
	
	/**
	 * Adds the franchise to the dealer location tables
	 * @param vo
	 * @throws Exception
	 */
	public void addDealerLocation(SMTServletRequest req, String insert) 
	throws ActionException, SQLException {
		SMTActionInterface sai = new DealerInfoAction(this.actionInit);
		//String localization = StringUtil.checkVal(((SiteVO)req.getAttribute("siteData")).getLocale());

		// Append the FS_ to the entered dealer id
		String dealerId = "FS_" + req.getParameter("dealerId");
		req.setParameter("dealerId", dealerId);
		
		// Add Dealer
		String origOrgId = req.getParameter("organizationId");
		req.setParameter("organizationId", FS_SITE_ID);
		req.setParameter("insertAction", Boolean.toString(!dealerExists(dealerId)));
		req.setParameter("dealerSubmitted", "true");
		req.setParameter("dealerTypeId", "3");
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.update(req);
		
		// get default location desc
		req.setParameter("locationDesc", this.getLocationDesc(req));
		
		// Add Dealer Location
		//req.setParameter("country", localization.substring(localization.indexOf("_")+1, localization.length()));
		req.setParameter("insertAction", insert);
		req.setParameter("dealerSubmitted", "false");
		req.setParameter("locationSubmitted", "true");
		req.setParameter("locationName", req.getParameter("dealerName"));
		req.setParameter("activeFlag", "0");
		req.setParameter("attrib2Text", "FASTSIGNS");
		sai.update(req);
		
		// Set the org id back to the franchise org id
		req.setParameter("organizationId", origOrgId);
	}
	
	/**
	 * This method adds the center organization to the database.
	 * @param vo
	 */
	public void addOrganization(FranchiseVO vo, SMTServletRequest req) throws Exception {
		SMTActionInterface sai = new OrganizationAction(this.actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		req.setParameter("actionId", "ORG");
		req.setParameter("organizationName", "FASTSIGNS " + vo.getLocationName());
		req.setParameter("organizationId", FS_SITE_ID + "_" + vo.getFranchiseId());
		req.setParameter("organizationGroupId", FS_GROUP);
		req.setParameter("allModuleFlag", "0");
		sai.update(req);
	}
	
	public String addSecondaryLayout(SMTServletRequest req) throws Exception {
		log.debug("siteId = " + req.getParameter("siteId"));
		SMTActionInterface sai = new TemplateAction(this.actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		req.setParameter("actionId", "TEMPLATE");
		req.setParameter("pageModuleId", "");
		req.setParameter("pmTemplateId", "");
		req.setParameter("organizationId", req.getParameter("organizationId"));
		req.setParameter("columns", "3");
		req.setParameter("siteId", req.getParameter("siteId"));
		req.setParameter("layoutName", "Secondary Page Layout");
		req.setParameter("pageTitle", "Welcome to FASTSIGNS &reg;");
		req.setParameter("defaultColumn", "2");
		req.setParameter("numberColumns", "3");
		req.setParameter("defaultFlag", "0");
		req.setParameter("templateId", "");
		req.setParameter("paramName", "");
		sai.update(req);
		
		return this.getSecondaryLayoutId(req.getParameter("siteId"), "Secondary Page Layout");
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.fastsigns.action.wizard.SiteWizardAction#addSingleColLayout(com.siliconmtn.http.SMTServletRequest)
	 */
	public String addEmptyColLayout(SMTServletRequest req) throws Exception {
		SMTActionInterface sai = new TemplateAction(this.actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		req.setParameter("actionId", "TEMPLATE");
		req.setParameter("pageModuleId", "");
		req.setParameter("pmTemplateId", "");
		req.setParameter("columns", "1");
		req.setParameter("layoutName", EMPTY_COL_LABEL);
		req.setParameter("pageTitle", "Welcome to FASTSIGNS &reg;");
		req.setParameter("defaultFlag", "0");
		req.setParameter("defaultColumn","1");
		req.setParameter("numberColumns", "1");
		req.setParameter("templateId", "");
		req.setParameter("paramName", "");
		sai.update(req);
		
		return getSecondaryLayoutId(req.getParameter("siteId"), EMPTY_COL_LABEL);
	}
}
