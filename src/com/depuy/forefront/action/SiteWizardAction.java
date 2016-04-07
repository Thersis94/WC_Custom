package com.depuy.forefront.action;
// JDK 1.6.x
import java.io.IOException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.depuy.forefront.action.vo.HospitalInstanceVO;
import com.depuy.forefront.action.vo.HospitalVO;
import com.depuy.forefront.action.vo.ProgramVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.admin.action.OrganizationAction;
import com.smt.sitebuilder.admin.action.SiteAction;
import com.smt.sitebuilder.admin.action.SitePageAction;
import com.smt.sitebuilder.admin.action.data.PageModuleVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
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
public abstract class SiteWizardAction extends SBActionAdapter {
	
	// Hours, 3 Button, center image and text, Modules and Map
	public List<PageModuleVO> defDisplay = new LinkedList<PageModuleVO>();
	public List<String> pageIds = new ArrayList<String>();
	/*
	 * These are variables set in the localization bundles for country specific id's
	 */
	public String pilotSiteId = "PILOT_1";
	public String pilotGroup = "FOREFRONT";
	
	/*
	 * These are the messages sent back to the user, set in the localization bundle for each country.
	 */
	public String posMsg1 = "You have successfully created the site: ";
	public String negMsg1 = "Unable to add new site: ";
	public String negMsg2 = ".  Please contact the system administrator for assistance";
	public String negMsg3 = " because it already exists";
	public String negMsg4 = "Unable to add new site. ";
	
	/**
	 * Default Constructor.
	 */
	public SiteWizardAction() {
	}
	
	/**
	 * @param actionInit
	 */
	public SiteWizardAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("Starting Site Wizard");
		String msg = negMsg1 + ": " + req.getParameter("hospitalName") + negMsg3;
		attributes.put(AdminConstants.ADMIN_MODULE_DATA, new ModuleVO());
		HospitalInstanceVO vo = new HospitalInstanceVO(req);
		vo.setHospitalVO(new HospitalVO(req));
		vo.setProgramVO(new ProgramVO(req));
		log.debug("Hospital VO: " + vo);
		
		try {
			// Check to make sure the franchise doesn't exist
			this.checkForExistingEntry(req.getParameter("hostpitalName"));
			
			if(StringUtil.checkVal(vo.getHospitalVO().getHospitalId()).length() == 0)
				this.addHospital(req, vo);
			
			this.updateHospitalInst(req, vo);
			// Create org
			this.addOrganization(vo, req);
			
			// Create website
			this.addWebsite(vo, req);		
			
			// Update Layout information and add the secondary layout
			String layoutId = this.updateLayout(vo.getHospitalAlias());
			
			// Change the theme from the default to the new theme
			this.assignTheme(vo);
			
			// Add the home page using the default layout
			this.addMyRoutinePage(layoutId, req);
			pageIds.add(req.getParameter("pageId"));
			this.addMyActionPlanPage(layoutId, req);
			pageIds.add(req.getParameter("pageId"));
			this.addMyMilestonesPage(layoutId, req);
			pageIds.add(req.getParameter("pageId"));
			
			// Assign the display types
			this.assignTypes();
						
			this.assignPageModules(req, layoutId);
			
			msg = posMsg1 + req.getParameter("dealerName");
		} catch(InvalidDataException ide) {
			log.error("Unable to add new website for franchise",ide);
			msg = negMsg1 + ": " + req.getParameter("dealerName") + negMsg3;
		} catch(NumberFormatException e) {
			log.error("Franchise dealer ID contains letters and cannot be used",e);
			msg = negMsg4 + req.getParameter("dealerLocationId") +", " + req.getParameter("dealerId");
		} catch (Exception e) {
			log.error("Unable to add new website for franchise",e);
			msg = negMsg1 + req.getParameter("dealerName") + negMsg2;
		}
		
		
		// Flush Cache
		this.clearCacheByGroup(req.getParameter("organizationId") + "_1");
		
		// Build the redirect
		
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			this.sendRedirect(page.getFullPath(), msg, req);
		
	}
	
	public void addHospital(SMTServletRequest req, HospitalInstanceVO vo){
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FOREFRONT_HOSPITAL (HOSPITAL_NM, CREATE_DT, HOSPITAL_ID) values");
		sb.append("(?,?,?)");
		vo.getHospitalVO().setHospitalId(new UUIDGenerator().getUUID());
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, vo.getHospitalVO().getHospitalName());
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, vo.getHospitalVO().getHospitalId());
			ps.executeUpdate();
		} catch(SQLException e){
			log.error("There was a problem inserting the hospital specified.", e);
		} finally{ try{
			ps.close();
		} catch(Exception e){
			
		}}
	}
	
	public void updateHospitalInst(SMTServletRequest req, HospitalInstanceVO vo){
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FOREFRONT_HOSPITAL_INST (HOSPITAL_ID, PROGRAM_ID, SITE_ID, ");
		sb.append("CREATE_DT, HOSPITAL_INST_ID) values(?,?,?,?,?)");
		vo.setHospitalInstId(new UUIDGenerator().getUUID());
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, vo.getHospitalId());
			ps.setString(2, vo.getProgramId());
			ps.setString(3,  vo.getHospitalAlias());
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.setString(5, vo.getHospitalInstId());
			ps.executeUpdate();
		} catch(SQLException e){
			log.error("There was a problem inserting the hospital specified.", e);
		} finally{ try{
			ps.close();
		} catch(Exception e){
			
		}}
	}
	
	/**
	 * This method checks to see if a franchise, organization, or site exists for the given id.
	 * @param id
	 * @throws SQLException
	 * @throws InvalidDataException
	 */
	public void checkForExistingEntry(String id) throws SQLException, InvalidDataException, NumberFormatException {
		Integer.parseInt(id);
		StringBuilder s = new StringBuilder();
		s.append("select organization_id from organization where organization_id like ? ");
		s.append("union ");
		s.append("select site_id from site where site_id like ? ");
		
		log.debug("Hospital Check SQL: " + s + "|" + id);
		
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setString(1, "%" + id + "%");
		ps.setString(2, "%" + id + "%");
		ps.setInt(3, Convert.formatInteger(id));
		
		ResultSet rs = ps.executeQuery();
		if (rs.next()) throw new InvalidDataException("Hospital Already Exists");
		ps.close();
	}
	
	/**
	 * This method adds the home page to the center site. Implemented in 
	 * localized site wizard.
	 * @param layoutId
	 * @param page
	 */
	public void addMyRoutinePage(String layoutId, SMTServletRequest req) 
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
		req.setParameter("aliasName", "MyRoutine");
		req.setParameter("displayName", "My Routine");
		req.setParameter("titleName", "My Routine");
		req.setParameter("metaKeyword", "");
		req.setParameter("metaDesc", "");
		req.setParameter("visible", "0");
		req.setParameter("defaultPage", "1");
		req.setParameter("orderNumber", "1");
		req.setParameter("externalPageUrl", "");
		req.setParameter("roles", new String[] {"0", "10", "100"}, true);
		sai.update(req);
	}
	
	public void addMyActionPlanPage(String layoutId, SMTServletRequest req) 
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
		req.setParameter("aliasName", "MyActionPlan");
		req.setParameter("displayName", "My Action Plan");
		req.setParameter("titleName", "My Action Plan");
		req.setParameter("metaKeyword", "");
		req.setParameter("metaDesc", "");
		req.setParameter("visible", "1");
		req.setParameter("defaultPage", "1");
		req.setParameter("orderNumber", "2");
		req.setParameter("externalPageUrl", "");
		req.setParameter("roles", new String[] {"0", "10", "100"}, true);
		sai.update(req);
	}
	
	public void addMyMilestonesPage(String layoutId, SMTServletRequest req) 
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
		req.setParameter("aliasName", "MyMilestones");
		req.setParameter("displayName", "My Milestones");
		req.setParameter("titleName", "My Milestones");
		req.setParameter("metaKeyword", "");
		req.setParameter("metaDesc", "");
		req.setParameter("visible", "1");
		req.setParameter("defaultPage", "1");
		req.setParameter("orderNumber", "3");
		req.setParameter("externalPageUrl", "");
		req.setParameter("roles", new String[] {"0", "10", "100"}, true);
		sai.update(req);
	}
	
	/**
	 * This method assigns the themes to the center site. Implemented in 
	 * localized site wizard.
	 * @param siteId
	 */
	public void assignTheme(HospitalInstanceVO vo) throws Exception {
		String siteId = "PILOT_" + vo.getHospitalAlias() + "_1";
		
		String sql = "update site_theme_impl set theme_menu_id = 'EMPTY_MENU',";
		sql += "theme_stylesheet_id = 'EMPTY_CSS1' ";
		sql += "where site_id = ?";
		log.debug("Theme Update: " + sql + "|" + siteId);
		
		PreparedStatement ps = dbConn.prepareStatement(sql);
		ps.setString(1, siteId);
		ps.executeUpdate();
	}	
	
	/**
	 * Modifies the number of columns and the default column for the layout.
	 * @param fId
	 * @return GUID for the layout
	 */
	public String updateLayout(String hId) throws SQLException {
		String siteId = "PILOT_" + hId + "_1";
		String tIdSql = "select template_id from template where site_id = '" + siteId + "'";
		String tId = null;
		
		Statement s = dbConn.createStatement();
		ResultSet rs = s.executeQuery(tIdSql);
		if (rs.next()) tId = rs.getString(1);
		
		String sql = "update template set columns_no=1, default_column_no=1";
		sql += "where template_id = '" + tId + "'";
		
		s = dbConn.createStatement();
		s.executeUpdate(sql);
		
		s.close();
		
		// Associate the Center hours, buttons and small map in right column
		
		return tId;
	}
	
	/**
	 * Adds the franchise web site for the provided franchise.
	 * @param vo
	 * @throws IOException
	 */
	public void addWebsite(HospitalInstanceVO vo, SMTServletRequest req) throws Exception {
		String email = req.getParameter("contactEmail");
		SMTActionInterface sai = new SiteAction(this.actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		req.setParameter("actionId", "SITE");
		req.setParameter("noEmailNote", "true");
		req.setParameter("organizationId", "PILOT_" + vo.getHospitalAlias());
		req.setParameter("siteName", vo.getHospitalVO().getHospitalName());
		req.setParameter("adminEmail", email);
		req.setParameter("adminName", "Webmaster");
		req.setParameter("mainEmail", email);
		req.setParameter("languageCode", "en");
		req.setParameter("allowAliasPathFlag", "0");
		req.setParameter("aliasPathName", vo.getHospitalAlias());
		req.setParameter("aliasPathParentId", pilotSiteId);
		req.setParameter("documentPath", null);
		sai.update(req);
	}
	
	/**
	 * This method adds the hospital organization to the database.
	 * @param vo
	 */
	public void addOrganization(HospitalInstanceVO vo, SMTServletRequest req) throws Exception {
		SMTActionInterface sai = new OrganizationAction(this.actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		req.setParameter("actionId", "ORG");
		req.setParameter("organizationName", "Pilot " + vo.getHospitalVO().getHospitalName());
		req.setParameter("organizationId", "PILOT_" + vo.getHospitalAlias());
		req.setParameter("organizationGroupId", pilotGroup);
		req.setParameter("allModuleFlag", "0");
		sai.update(req);
	}
	/**
	 * This method assigns the page modules to the center pages. Implemented in 
	 * localized site wizard.
	 */
	public void assignTypes() {
		
		//TODO update Module Display Ids
		defDisplay.add(makePageModule("c0a802375aba1ee7516cbb5cbebeae52","0a00141321debe7b60f744fc72737af", "FF_MY_ACTIONPLAN_ACTION", 0, 1));
		defDisplay.add(makePageModule("c0a802375ab8d3c012dfd6e23d920a4e","c0a802375a95ba799220ae25eef14058", "FOREFRONT_MY_PROGRESS", 0, 1));
		defDisplay.add(makePageModule("c0a802375a95ba799220ae25eef14058","0a00141321debe7b60f744fc72737af", "FF_PROGRESS_ACTION", 1, 3));

			}
	
	/**
	 * This method assists in creating the page modules.
	 * @param actionId
	 * @param displayPgId
	 * @param paramNm
	 * @param col
	 * @param order
	 * @return
	 */
	public PageModuleVO makePageModule(String actionId, String displayPgId, String paramNm, int col, int order) {
		PageModuleVO pm = new PageModuleVO();
		pm.setActionId(actionId);
		pm.setModuleDisplayId(displayPgId);
		pm.setParamName(paramNm);
		pm.setDisplayColumn(col);
		pm.setDisplayOrder(order);
		pm.setRoles(makeRoles(true, true, true)); //add default Public roles, some modules will override this
		return pm;
	}
	
	/**
	 * This method associates the proper roles to the site. Implemented in 
	 * localized site wizard.
	 * @param isPublic
	 * @param isReg
	 * @param isAdmin
	 * @return
	 */
	public Map<String, Integer> makeRoles(boolean isPublic, boolean isReg, boolean isAdmin) {
		Map<String, Integer> roles = new HashMap<String, Integer>();
		
		if (isPublic) roles.put("0", 0);
		
		if (isReg) roles.put("10", 10);
		
		if (isAdmin) roles.put("100", 100);
		
		return roles;
	}
	
	public void assignPageModules(SMTServletRequest req, String layoutId){
		for (int i = 0; i < defDisplay.size(); i++) {
				PageModuleVO vo = defDisplay.get(i);
				String pageId = pageIds.get(i);
            String pageModuleId = new UUIDGenerator().getUUID();
            StringBuilder sb = new StringBuilder();
            sb.append("insert into page_module ");
            sb.append("(module_display_id,template_id,action_id, display_column_no,");
            sb.append("order_no, module_action_nm, param_nm, create_dt, pageId, page_module_id) ");
            sb.append("values (?,?,?,?,?,?,?,?,?,?)");
			
			PreparedStatement ps = null;
	        try {
	            ps = dbConn.prepareStatement(sb.toString());
	            ps.setString(1, vo.getModuleDisplayId());
	            ps.setString(2, layoutId);
	            ps.setString(3, vo.getActionId());
	            ps.setInt(4, vo.getDisplayColumn());
	            ps.setInt(5, vo.getDisplayOrder());
	            ps.setString(6, null);
	            ps.setString(7, vo.getParamName());
	            ps.setTimestamp(8, Convert.getCurrentTimestamp());
	            ps.setString(9, pageId);
	            ps.setString(10, pageModuleId);
	            ps.executeUpdate();
	            
	    		StringBuffer sql = new StringBuffer();
	    		sql.append("insert into page_module_role(page_module_role_id, page_module_id, role_id, create_dt) ");
	    		sql.append("values (?,?,?,?)");
	    		
	    		
	            for (String role : vo.getRoles().keySet()) {
	            	ps = dbConn.prepareStatement(sql.toString());
	                ps.setString(1, new UUIDGenerator().getUUID());
	                ps.setString(2, pageModuleId);
	                ps.setString(3, role);
	                ps.setTimestamp(4, Convert.getCurrentTimestamp());
	                ps.executeUpdate();
	            }
	        } catch (SQLException e) {
				e.printStackTrace();
			} finally {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {	}
	        }
		}
	}
}
