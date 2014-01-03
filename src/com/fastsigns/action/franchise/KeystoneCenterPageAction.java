/**
 * 
 */
package com.fastsigns.action.franchise;

import javax.servlet.http.HttpSession;


import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRoleContainer;

/****************************************************************************
 * <b>Title</b>: KeystoneCenterPageAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 19, 2011
 ****************************************************************************/
public class KeystoneCenterPageAction extends CenterPageAction {

	public KeystoneCenterPageAction() {
		
	}

	public KeystoneCenterPageAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/**
	 * THIS ACTION EXISTS TO PROXY THE SUPERCLASS AND NOT BE CACHED BY MODULE CONTROLLER.
	 * THE SUPERCLASS IS CACHED AND CALLED FROM THE PUBLIC SITE (DIRECTLY); WHERE-AS
	 * THIS ACTION IS NOT CACHED AND CALLED FROM THE KEYSTONE SITE (DIRECTLY).
	 */
	
	
	/**
	 * pass a flag to the retrieve method to bypass the APPROVAL_FLG constraint.
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("Starting center page retrieve");
		this.polymorphRoles(req);
        
		req.setAttribute("isKeystone", Boolean.TRUE);
		super.retrieve(req);
	}
	
	
	/**
	 * this method transforms the public-site Roles stored in sessionScope to be 
	 * in the Admintool's roleData format.
	 * This empowers the WYSIWYG's FileManager to function.
	 */
	protected void polymorphRoles(SMTServletRequest req) {
		//this snippet allows Keystone admins to use the Admintool's file manager
		HttpSession ses = req.getSession();
    	SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
    	
        SBUserRoleContainer roles = (SBUserRoleContainer) ses.getAttribute(AdminConstants.ADMIN_ROLE_DATA);
        String franOrgId = site.getOrganizationId() + "_" + StringUtil.checkVal(CenterPageAction.getFranchiseId(req));
        if (roles == null || roles.getRole(franOrgId) == null) {
        	UserRoleVO role = (UserRoleVO) ses.getAttribute(Constants.ROLE_DATA);
        	if (role != null && role.getRoleLevel() >= 30) {
	        	if (roles == null) roles = new SBUserRoleContainer();
	        	
	        	//give superusers full access to the parent org "FTS", this also gives them FULL admintool authority (for FTS)
	        	//if (role.getRoleLevel() == SecurityController.ADMIN_ROLE_LEVEL)
	        	//changed to give all users FTS access, this empowers FileManager to function in read-only mode - JM 06/03/11
	        	roles.addRole(site.getOrganizationId(), role);
	        	
	        	//give all users access to the Franchise they're editing
	        	roles.addRole(franOrgId, role);
	        	
	        	roles.setAuthorized(true);
	        	ses.setAttribute(AdminConstants.ADMIN_ROLE_DATA, roles);
	        	//log.debug("set roles:" + roles.getUserRoles());
        	}
        }
	}
	
	
	/**
	 * POLYMORPHISM OVER THE SUPERCLASS; the query used to retrieve the module data
	 * This query gets the data native to the core/system, where-as the superclass
	 * gets the data assigned to this Franchise.
	 * (query->all-data  -or-  query->assigned-data)
	 */
	protected StringBuilder formatQuery(SMTServletRequest req) {
		req.setAttribute(Constants.PAGE_PREVIEW, Convert.formatBoolean(req.getParameter("reloadMenu"), false));
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		String orgId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();
		boolean isMobile = Convert.formatBoolean(req.getSession().getAttribute("webeditIsMobile"));
		//without a module_location the below query will fail.  Return the default "grab all as layout" query
		if (Convert.formatInteger(req.getParameter("locationId")) == 0) 
			return super.formatQuery(req);
		
		s.append("select *, c.create_dt as 'option_create_dt', c.franchise_id as 'option_franchise_id', ");
		s.append("c.cp_module_option_id as 'mod_opt_id', e.order_no as attr_order_no, d.CP_MODULE_FRANCHISE_XR_ID as franXr ");
		s.append("from ").append(customDb);
		s.append("fts_cp_module_type_xr a ");
		s.append("inner join ").append(customDb).append("fts_cp_module_type b ");
		s.append("on a.fts_cp_module_type_id = b.fts_cp_module_type_id ");
		s.append("inner join ").append(customDb).append("fts_cp_module_option c ");
		s.append("on b.fts_cp_module_type_id = c.fts_cp_module_type_id ");
		s.append("inner join ").append(customDb).append("FTS_CP_LOCATION_MODULE_XR f ");
		s.append("on f.CP_LOCATION_MODULE_XR_ID = ? ");
		s.append("inner join ").append(customDb).append("FTS_CP_MODULE g ");
		s.append("on g.CP_MODULE_ID = f.CP_MODULE_ID ");
		if(isMobile)
			s.append("and g.MOBILE_FLG = 1 ");
		else
			s.append("and (g.MOBILE_FLG = 0 or g.MOBILE_FLG is null) ");
		s.append("left outer join ").append(customDb).append("fts_cp_module_franchise_xr d ");
		s.append("on d.cp_location_module_xr_id = ? and ");
		s.append("(c.cp_module_option_id=d.cp_module_option_id or c.parent_id=d.cp_module_option_id) ");
		s.append("left outer join ").append(customDb).append("FTS_CP_OPTION_ATTR e on c.CP_MODULE_OPTION_ID ");
		s.append("= e.cp_module_option_id ");
		s.append("where a.cp_module_id=? and (c.franchise_id is null or c.franchise_id = ?) and c.org_id = '");
		s.append(orgId).append("' ");
		if (req.getParameter("optionId") != null) 
			s.append("and c.cp_module_option_id=? ");
		
		if (Convert.formatBoolean(req.getParameter("order"))) {
			//the "reorder" thickbox view...order the modules how the user previously ordered them.
			s.append("order by b.fts_cp_module_type_id, d.order_no, c.option_nm");
		} else {
			s.append("order by b.fts_cp_module_type_id, case b.fts_cp_module_type_id when 2 then c.start_dt else getDate() end desc, c.option_nm, d.order_no");
		}
		
		log.debug("KEYSTONE CP Module Retrieve SQL: " + s);
		return s;
	}
}
