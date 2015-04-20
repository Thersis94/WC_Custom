package com.fastsigns.action.wizard.mobile;

import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import com.fastsigns.action.franchise.centerpage.FranchiseLocationInfoAction;
import com.fastsigns.action.franchise.vo.FranchiseVO;
import com.fastsigns.action.wizard.FSSiteWizardIntfc;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.admin.action.SiteAction;
import com.smt.sitebuilder.admin.action.data.PageModuleVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

public abstract class MobileSiteWizardAction extends SBActionAdapter implements FSSiteWizardIntfc {
	
	// Hours, 3 Button, center image and text, Modules and Map
	public List<PageModuleVO> defDisplay = new LinkedList<PageModuleVO>();
	public List<PageModuleVO> secDisplay = new LinkedList<PageModuleVO>();

	/*
	 * These are variables set in the localization bundles for country specific id's
	 */
	public String FS_SITE_ID = "FTS";
	public String FS_GROUP = "FAST_SIGNS";
	public String emailSuffix = "@fastsigns.com";
	
	/*
	 * These are the messages sent back to the user, set in the localization bundle for each country.
	 */
	public String posMsg1 = "You have successfully created the mobile site for center: ";
	public String posMsg2 = "You have successfully updated the mobile site: ";
	public String negMsg1 = "Unable to add new mobile site: ";
	public String negMsg2 = ".  Please contact the system administrator for assistance";
	public String negMsg3 = " because it already exists";
	
	/**
	 * Default Constructor.
	 */
	public MobileSiteWizardAction() {
		
	}
	
	/**
	 * @param actionInit
	 */
	public MobileSiteWizardAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		
		// Assign the display types
		this.assignTypes();

		String msg = negMsg1 + ": " + req.getParameter("dealerLocId") + negMsg3;
		try {
			if(!Convert.formatBoolean(req.getParameter("isWizard"))){
				dbConn.setAutoCommit(false);
			}
			insertOrReturn(req);
			msg = posMsg1 + req.getParameter("dealerLocId");
			if(!Convert.formatBoolean(req.getParameter("isWizard"))){
				dbConn.commit();
				dbConn.setAutoCommit(true);
			}

		} catch(InvalidDataException ide) {
			log.error("Unable to add new website for franchise",ide);
			msg = negMsg1 + ": " + req.getParameter("dealerLocId") + negMsg3;
		} catch(SQLException sqle) {
			log.error("Location already exists", sqle);
			msg = negMsg1 + req.getParameter("dealerLocId") + negMsg3;
		} catch (Exception e) {
			log.error("Unable to add new website for franchise",e);
			msg = negMsg1 + req.getParameter("dealerLocId") + negMsg2;
		}
		
		// Flush Cache
		this.clearCacheByGroup(req.getParameter("organizationId") + "_2");
		
		// Build the redirect
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			this.sendRedirect(page.getFullPath(), msg, req);
	}
		
	/* (non-Javadoc)
	 * @see com.fastsigns.action.wizard.FSSiteWizardIntfc#insertOrReturn(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void insertOrReturn(SMTServletRequest req) throws Exception {
		log.debug("Starting Site Wizard");
		attributes.put(AdminConstants.ADMIN_MODULE_DATA, new ModuleVO());
		// Prevent approval from getting in the way of page creation.
		req.setParameter("skipApproval", "true");
		String dealerLocId = (String) req.getParameter("dealerLocId");
		FranchiseVO vo = getFranchiseData(dealerLocId);
		
		log.debug("Franchise VO: " + vo + ", aliasPath = " + vo.getLocationAlias());
		String franchiseId = vo.getFranchiseId();

		// Check to make sure the franchise doesn't exist when no change is selected.
		this.checkForExistingEntry(dealerLocId);

		// Add the center page portlet
		String centerActionId = addCenterPage(franchiseId);
		
		// Create website
		this.addWebsite(vo, req);
		
		// Update Layout information and add the secondary layout
		String layoutId = this.updateLayout(franchiseId, centerActionId);
		String secLayoutId = this.addSecondaryLayout(req);
		
		// Associate the main modules and the center image/text to the layouts
		associateCenterPage(layoutId, vo.getFranchiseId(), centerActionId, 1);
		associateCenterPage(secLayoutId, vo.getFranchiseId(), centerActionId, 2);

		// Associate the main modules and the center image/text to the layouts
		//associateCenterPage(layoutId, franchiseId, centerActionId, 1);
		
		// Change the theme from the default to the new theme
		this.assignTheme(vo); 
		
		// Add the home page using the default layout
		this.addHomePage(layoutId, franchiseId, req);
		
		// Add a redirect to the system for the franchise to alias redirect
		this.addRedirect(franchiseId, vo.getLocationAlias(), req);
		
	}
	
	/**
	 * Returns the FranchiseVO with all the populated data that we would have gotten
	 * off the form.
	 * @param dealerLocId
	 * @return
	 */
	public FranchiseVO getFranchiseData(String dealerLocId) {
		FranchiseLocationInfoAction fla = new FranchiseLocationInfoAction(this.actionInit);
		fla.setDBConnection(dbConn);
		fla.setAttributes(attributes);
		return fla.getLocationInfo(dealerLocId, true);
	}

	/* (non-Javadoc)
	 * @see com.fastsigns.action.wizard.FSSiteWizardIntfc#checkForExistingEntry(java.lang.String)
	 */
	@Override
	public void checkForExistingEntry(String id) throws SQLException, InvalidDataException, NumberFormatException {
		Integer.parseInt(id);
		StringBuilder s = new StringBuilder();
		s.append("select site_id from site where site_id = ? ");		
		log.debug("Franchise Check SQL: " + s + "|" + id);
		
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setString(1, FS_SITE_ID + "_"+ id + "_2");
				
		ResultSet rs = ps.executeQuery();
		if (rs.next()) throw new InvalidDataException("Mobile Site Already Exists");
		ps.close();
	}
	
	/* (non-Javadoc)
	 * @see com.fastsigns.action.wizard.FSSiteWizardIntfc#associateCenterPage(java.lang.String, java.lang.String, java.lang.String, int)
	 */
	@Override
	public void associateCenterPage(String layoutId, String fId, String centerActionId, int type) 
	throws Exception {
		List<PageModuleVO> current = secDisplay;
		if (type == 1) current = defDisplay;
		log.debug("***********************: " + secDisplay.size() + "|" + defDisplay.size() + "|" + current.size());
			
			for (PageModuleVO vo : current) {
				
				if (vo.getActionId() == null)
					vo.setActionId(centerActionId);
				
				log.debug(vo.getParamName());
	            String pageModuleId = new UUIDGenerator().getUUID();
	            StringBuilder sb = new StringBuilder();
	            sb.append("insert into page_module ");
	            sb.append("(module_display_id,template_id,action_id, display_column_no,");
	            sb.append("order_no, module_action_nm, param_nm, create_dt, page_module_id) ");
	            sb.append("values (?,?,?,?,?,?,?,?,?)");
				
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
		            ps.setString(9, pageModuleId);
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
		        } finally {
		        	try {
		        		ps.close();
		        	} catch(Exception e) {	}
		        }
			}
	}
	
	/* (non-Javadoc)
	 * @see com.fastsigns.action.wizard.FSSiteWizardIntfc#updateLayout(java.lang.String, java.lang.String)
	 */
	@Override
	public String updateLayout(String fId, String actionId) throws SQLException {
		String siteId = FS_SITE_ID + "_" + fId + "_2";
		String tIdSql = "select template_id from template where site_id = '" + siteId + "'";
		String tId = null;
		
		Statement s = dbConn.createStatement();
		ResultSet rs = s.executeQuery(tIdSql);
		if (rs.next()) tId = rs.getString(1);
		
		s.close();
		
		// Associate the Center hours, buttons and small map in right column
		
		return tId;
	}
	
	/* (non-Javadoc)
	 * @see com.fastsigns.action.wizard.FSSiteWizardIntfc#addWebsite(com.fastsigns.action.franchise.vo.FranchiseVO, com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void addWebsite(FranchiseVO vo, SMTServletRequest req) throws Exception {
		String email = vo.getFranchiseId() + emailSuffix;
		String dp = URLEncoder.encode("/cms/main.jsp", "UTF-8");
		SMTActionInterface sai = new SiteAction(this.actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		req.setParameter("actionId", "SITE");
		req.setParameter("noEmailNote", "true");
		req.setParameter("organizationId", FS_SITE_ID + "_" + vo.getFranchiseId());
		req.setParameter("siteName", "Mobile Center Page " + vo.getFranchiseId());
		req.setParameter("adminEmail", email);
		req.setParameter("adminName", "Webmaster");
		req.setParameter("mainEmail", email);
		req.setParameter("languageCode", "en");
		req.setParameter("allowAliasPathFlag", "0");
		req.setParameter("aliasPathName", vo.getLocationAlias());
		req.setParameter("aliasPathParentId", FS_SITE_ID + "_4");
		req.setParameter("documentPath", dp);
		req.setParameter("mobileFlag", "1");
		
		sai.update(req);
	}
		
	/* (non-Javadoc)
	 * @see com.fastsigns.action.wizard.FSSiteWizardIntfc#makePageModule(boolean, boolean, boolean, java.lang.String, java.lang.String, java.lang.String, int, int)
	 */
	@Override
	public PageModuleVO makePageModule(boolean isPublic, boolean isRegistered, boolean isAdmin, String actionId, String displayPgId, String paramNm, int col, int order) {
		PageModuleVO pm = new PageModuleVO();
		pm.setActionId(actionId);
		pm.setModuleDisplayId(displayPgId);
		pm.setParamName(paramNm);
		pm.setDisplayColumn(col);
		pm.setDisplayOrder(order);
		pm.setRoles(makeRoles(isPublic, isRegistered, isAdmin)); //add default Public roles, some modules will override this
		return pm;
	}

	@Override
	public String getSecondaryLayoutId(String siteId, String name)
			throws Exception {
		String sql = "select template_id from template where site_id = ? and layout_nm = ?";
		//log.debug("Get Sec Template ID SQL: " + sql + "|" + siteID + "|" + alias);
		
		PreparedStatement ps = dbConn.prepareStatement(sql);
		String tId = "";
		ps.setString(1, siteId);
		ps.setString(2, name);
		ResultSet rs = ps.executeQuery();
		if (rs.next()) tId = rs.getString(1);
		
		return tId;
	}
	
	@Override
	public String addCenterPage(String franchiseId) throws SQLException {
		String s = "insert into sb_action (action_nm, action_desc, organization_id, ";
		s += "module_type_id, action_id, attrib1_txt, create_dt) values (?,?,?,?,?,?,?)";
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s);
			ps.setString(1, "Center Page Portlet");
			ps.setString(2, "Center Page Portlet");
			ps.setString(3, FS_SITE_ID + "_"+ franchiseId);
			ps.setString(4, "FTS_CENTER_PAGE");
			ps.setString(5, FS_SITE_ID + "_CENTER_PAGE_" + franchiseId + "_2");
			ps.setString(6, franchiseId);
			ps.setTimestamp(7, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
		// Return the action id
		return FS_SITE_ID + "_CENTER_PAGE_" + franchiseId + "_2";
	}
	
	/**
	 * Update the current redirect for the locator to now redirect to our subsite.
	 * @param fId
	 * @param alias
	 * @param req
	 * @throws ActionException
	 */
	public void addRedirect(String fId, String alias, SMTServletRequest req) 
	throws ActionException {
		/*
		 * We are updating a redirect, not adding a new one.
		 */
		StringBuilder sql = new StringBuilder(90);
		sql.append("update SITE_REDIRECT set destination_url = ?, update_dt = ? where site_redirect_id = ?");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, "/" + alias);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, FS_SITE_ID + "_" + fId + "_MOBILE");
			ps.executeUpdate();
		} catch (Exception e) {
			log.debug("The site was unable to update the redirect for center" + fId, e);
		}
		
		sql = new StringBuilder(60);
		sql.append("delete from SITE_REDIRECT where site_redirect_id = ?");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, "FTS_" + fId + "_MOBILE_ALIAS");
			ps.executeUpdate();
		} catch (Exception e) {
			log.debug("The site was unable to update the redirect for center" + fId, e);
		}
	}

}
