package com.fastsigns.action.wizard.mobile;

// JDK 1.6.x
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// FASTSIGNS Libs
import com.fastsigns.action.franchise.vo.FranchiseVO;

// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

// WC Libs
import com.smt.sitebuilder.admin.action.SitePageAction;
import com.smt.sitebuilder.admin.action.TemplateAction;

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
public class MobileSiteWizardAction_GB extends MobileSiteWizardAction {
 
	/**
	 * Localization Text Set
	 */
	public MobileSiteWizardAction_GB() {
		FS_SITE_ID = "FTS_UK";
		FS_GROUP = "FAST_SIGNS_GB";

		posMsg1 = "You have successfully created the mobile site for centre: ";
		negMsg1 = "Unable to add new mobile site: ";
		negMsg2 = ".  Please contact the system administrator for assistance.";
		negMsg3 = " because it already exists";
	}

	/**
	 * Localization Text Set
	 * @param actionInit
	 */
	public MobileSiteWizardAction_GB(ActionInitVO actionInit) {
		super(actionInit);
		FS_SITE_ID = "FTS_UK";
		FS_GROUP = "FAST_SIGNS_UK";
		posMsg1 = "You have successfully created the mobile site: ";
		negMsg1 = "Unable to add new mobile site: ";
		negMsg2 = ".  Please contact the system administrator for assistance.";
		negMsg3 = " because it already exists";
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
		String siteId = FS_SITE_ID + "_" + vo.getFranchiseId() + "_2";
		
		String sql = "update site_theme_impl set theme_menu_id = '376680508C804ECD9B7D6EC1F16CEFDA',";
		sql += "theme_stylesheet_id = 'c0a802371c243ee7aee425eb1b83f053' ";
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
		//Primary pages (CP Based)
		defDisplay.add(makePageModule(true, true, true, "0a00141332afb0c76020f5cbba30d676","c0a80a07614b3c24224dd3d77221237a", "HEADER_LEFT", 0, 1));	//Mobile Header Left
		defDisplay.add(makePageModule(true, true, true, "0a00141332afb0c76020f5cb333c51f5","c0a80a07614b3c24224dd3d77221237a", "COPYRIGHT", 0, 1));		//Mobile Copyright
		//defDisplay.add(makePageModule(true, true, true, "0a00141332afb0c86020f5cb736bb561","c0a80a07614b3c24224dd3d77221237a", null, 0, 1));			//Mobile Find Location Bar
		defDisplay.add(makePageModule(true, true, true, null, "92ADB5AE0E404A6F9DFD29CB44216EAF", null, 1, 2));											//Mobile Header Bar	
		defDisplay.add(makePageModule(true, true, true, null, "c0a8016564a89155842b990af697746c", null, 1, 4));											//Modules 2012
		defDisplay.add(makePageModule(true, true, true, null, "5F9F98644CAE43E8B43A9B6C2B755DEF", null, 1, 6));											//Mobile Location Info
		
		
		// Secondary pages (Content Based)
		secDisplay.add(makePageModule(true, true, true, "0a00141332afb0c76020f5cbba30d676","c0a80a07614b3c24224dd3d77221237a", "HEADER_LEFT", 0, 1));	//Mobile Header Left
		secDisplay.add(makePageModule(true, true, true, "0a00141332afb0c76020f5cb333c51f5","c0a80a07614b3c24224dd3d77221237a", "COPYRIGHT", 0, 1));		//Mobile Copyright
		//secDisplay.add(makePageModule(true, true, true, "0a00141332afb0c86020f5cb736bb561","c0a80a07614b3c24224dd3d77221237a", null, 0, 1));			//Mobile Find Location Bar
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
	
	public String addSecondaryLayout(SMTServletRequest req) throws Exception {
		log.debug("siteId = " + req.getParameter("siteId"));
		SMTActionInterface sai = new TemplateAction(this.actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		req.setParameter("actionId", "TEMPLATE");
		req.setParameter("pageModuleId", "");
		req.setParameter("pmTemplateId", "");
		req.setParameter("organizationId", req.getParameter("organizationId"));
		req.setParameter("columns", "1");
		req.setParameter("siteId", req.getParameter("siteId"));
		req.setParameter("layoutName", "Secondary Page Layout");
		req.setParameter("pageTitle", "Welcome to FASTSIGNS &reg;");
		req.setParameter("defaultColumn", "1");
		req.setParameter("numberColumns", "1");
		req.setParameter("defaultFlag", "0");
		req.setParameter("templateId", "");
		req.setParameter("paramName", "");
		sai.update(req);
		
		return this.getSecondaryLayoutId(req.getParameter("siteId"), "Secondary Page Layout");
	}
}
