package com.fastsigns.action.wizard.ecommerce;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fastsigns.action.franchise.CenterPageAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.admin.action.SitePageAction;
import com.smt.sitebuilder.admin.action.data.PageModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

public class StorePageWizardAction extends SBActionAdapter{
	private StoreConfig conf = null;
	private List<PageModuleVO> modules = new LinkedList<PageModuleVO>();
	private String msg = "Error completing transaction";
	public StorePageWizardAction(){
		
	}
	
	public StorePageWizardAction(ActionInitVO ai){
		
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		try{
			req.setValidateInput(false);
			req.setParameter("templateId", makeEcommTemplate(req));
			boolean isDel = Convert.formatBoolean(req.getParameter("removeEcomm"), false);
			if(isDel)
				removeEcommerce(req);
			else
				addEcommerce(req);
			
			updateRolloutTable(req, isDel);
			updateLocationTable(req, isDel);
			updateRedirect(req, isDel);
			
			// Flush Cache
			this.clearCacheByGroup(req.getParameter("organizationId") + "_1");
		} catch (Exception e) {
			log.debug(e);
		}
		// Build the redirect
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			this.sendRedirect(page.getFullPath(), msg, req);
	}
	
	public void addEcommerce(SMTServletRequest req) throws ActionException {
		buildPage(req);
		addPageModules(req);
		msg = "Ecommerce Successfully added to website.";
	}
	
	public void removeEcommerce(SMTServletRequest req) throws ActionException {
		removePage(req);
		removeLayout(req);		
		msg = "Ecommerce Successfully removed from the website.";
	}
	
	public void updateLocationTable(SMTServletRequest req, boolean isDel) {
		StringBuilder sb = new StringBuilder();
		sb.append("update DEALER_LOCATION set ATTRIB1_TXT = ");
		if(isDel)
			sb.append("? ");
		else
			sb.append("LOCATION_ALIAS_NM + ? ");
		sb.append("where DEALER_LOCATION_ID = ?");
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sb.toString());
			if(isDel)
				ps.setString(1, "");
			else
				ps.setString(1, "/store");
			ps.setString(2, CenterPageAction.getFranchiseId(req));
			ps.executeUpdate();
		} catch(SQLException sql) {
			log.debug(sql);
		}
	}
	
	public void updateRolloutTable(SMTServletRequest req, boolean isDel) {
		StringBuilder sb = new StringBuilder();
		String customDb = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		if(isDel){
			sb.append("delete from ").append(customDb);
			sb.append("FTS_ECOMM_ROLLOUT where FRANCHISE_ID = ?");
		} else {
			sb.append("insert into ").append(customDb);
			sb.append("FTS_ECOMM_ROLLOUT (FRANCHISE_ID, USE_ECOMM_FLG, ");
			sb.append("CREATE_DT) values (?,?,?)");
		}
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, CenterPageAction.getFranchiseId(req));
			if(!isDel){
				ps.setInt(2, 1);
				ps.setTimestamp(3, Convert.getCurrentTimestamp());
			}
			ps.executeUpdate();
		} catch(SQLException sql){
			log.debug(sql);
		}
	}
	
	public void updateRedirect(SMTServletRequest req, boolean isDel) {
		StringBuilder sb = new StringBuilder();
		String franId = CenterPageAction.getFranchiseId(req);
		String aliasPathNm = getFranchiseAliasPath(franId);
		if(isDel){
			sb.append("insert into site_redirect (site_redirect_id, site_id, ");
			sb.append("redirect_alias_txt, destination_url, active_flg, global_flg, ");
			sb.append("permanent_redir_flg, log_redir_flg, create_Dt) values(?,?,?,?,?,?,?,?,?)");
		} else {
			sb.append("delete from SITE_REDIRECT where SITE_REDIRECT_ID = ?");
		}
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, conf.getOrgPrefix() + franId + "_ECOMM");
			if(isDel){
				ps.setString(2, conf.getOrgPrefix() + franId + "_1");
				ps.setString(3, "/" + aliasPathNm + "/store");
				ps.setString(4, "/" + aliasPathNm);
				ps.setInt(5, 1);
				ps.setInt(6, 0);
				ps.setInt(7, 1);
				ps.setInt(8, 0);
				ps.setTimestamp(9, Convert.getCurrentTimestamp());
			}
			ps.executeUpdate();
		} catch(SQLException sql){
			log.debug(sql);
		} finally{
			try{ps.close();}catch(Exception e){}
		}
	}
	
	private String getFranchiseAliasPath(String franId) {
		StringBuilder sb = new StringBuilder();
		sb.append("select LOCATION_ALIAS_NM from DEALER_LOCATION ");
		sb.append("where DEALER_LOCATION_ID=?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, franId);
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				return rs.getString(1);
		} catch(Exception e) {
			log.debug(e);
		}finally{
			try{ps.close();}catch(Exception e){}}
		return null;
	}

	public void buildPage(SMTServletRequest req) throws ActionException {
		SitePageAction spa = new SitePageAction(actionInit);
		spa.setDBConnection(dbConn);
		spa.setAttributes(attributes);
		req.setParameter("startDate", Convert.formatDate(new Date(), Convert.DATE_SLASH_PATTERN));
		req.setParameter("parentPath", "/");
		req.setParameter("organizationId", req.getParameter("organizationId"));
		req.setParameter("parentId", "");
		req.setParameter("siteId", req.getParameter("siteId"));
		req.setParameter("aliasName", "store");
		req.setParameter("displayName", "Product Store");
		req.setParameter("titleName", "Keystone-driven Product Store");
		req.setParameter("metaKeyword", "");
		req.setParameter("metaDesc", "");
		req.setParameter("visible", "1");
		req.setParameter("defaultPage", "0");
		req.setParameter("orderNumber", "1");
		req.setParameter("externalPageUrl", "");
		req.setParameter("roles", new String[] {"0", "10", "100", conf.getRegisteredRoleId(), "1000"}, true);
		req.setParameter("javaScript", conf.getJsText());
		spa.update(req);
	}
	
	public void removePage(SMTServletRequest req) throws ActionException {
		StringBuilder sb = new StringBuilder();
		sb.append("select * from PAGE where PAGE_ALIAS_NM='store' and SITE_ID=?");
		PreparedStatement ps = null;
		String pageId = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, conf.getOrgPrefix() + CenterPageAction.getFranchiseId(req) + "_1");
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				pageId = rs.getString("PAGE_ID");
		} catch(Exception e){
			
		} finally{
			try{ps.close();} catch(Exception e){}
		}
		if(pageId != null) {
			log.debug("deleting page: " + pageId);
			req.setParameter("pageId", pageId);
			SitePageAction spa = new SitePageAction(actionInit);
			spa.setDBConnection(dbConn);
			spa.setAttributes(attributes);
			spa.delete(req);
		}
	}
	
	public void removeLayout(SMTServletRequest req) throws ActionException {
		StringBuilder sb = new StringBuilder();
		sb.append("delete from TEMPLATE where LAYOUT_NM='eStore Layout' and SITE_ID=?");
		PreparedStatement ps = null;
		log.debug("deleting ecomm templates for: " + conf.getOrgPrefix() + CenterPageAction.getFranchiseId(req) + "_1");
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, conf.getOrgPrefix() + CenterPageAction.getFranchiseId(req) + "_1");
			ps.executeUpdate();
		} catch(Exception e){
			log.debug(e);
		} finally{
			try{ps.close();} catch(Exception e){}
		}
	}

	public void addPageModules(SMTServletRequest req) {
		makeModules(CenterPageAction.getFranchiseId(req));
		for (PageModuleVO vo : modules) {
			
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
	            ps.setString(2, req.getParameter("templateId"));
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
	        } catch (SQLException e) {
	        	log.debug(e);
			} finally {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {	}
	        }
		}
	}
	
	public void setConfig(StoreConfig conf) {
		this.conf = conf;
	}
	
	public void makeModules(String centerId) {
		modules.add(makePageModule("FTS_CENTER_PAGE_" + centerId, "c0a80247cf108e96173ab0fd1f615e6e", null, 2, 1));					// Center Page Portlet Center Page Portlet
		modules.add(makePageModule("FTS_CENTER_PAGE_" + centerId, "c0a80247628640b3c9be22729ba3fa2", null, 1, 2));					// Center Page Portlet Center Page Portlet
		modules.add(makePageModule("FTS_CENTER_PAGE_" + centerId, "c0a80223d97c7188a731646267e69b08", null, 1, 9));					// Need More Options Box
		modules.add(makePageModule(conf.getAnonHeaderId(),"c0a80a07614b3c24224dd3d77221237a", "HEADER_TOP_LEFT_CONTENT", 0, 1, false));	// World Link 2012 Anon
		modules.add(makePageModule(conf.getLogInHeaderId(),"c0a80a07614b3c24224dd3d77221237a", "HEADER_TOP_LEFT_CONTENT", 0, 1, true));	// World Link 2012 Logged In
		modules.add(makePageModule(conf.getSearchId(), "c0a8022d8f80712f35240b2c75ef14b2", "SITE_SEARCH", 1, 0));					// Site Search 2012
		modules.add(makePageModule(conf.getGalleryId(), "c0a802232ca672a7c9eacbe4b14a5364", null, 2, 3));							// 2012 Shared CenterPage Slider 2012 Shared CenterPage Slider
		modules.add(makePageModule(conf.getStoreId(), "c0a8024763afbf9d7e1dde6fa62eb12b", null, 0, 1));								// Checkout Default Checkout
		modules.add(makePageModule(conf.getECommId(), "7f00010190e8f072145d1aaf75a2fe03", null, 1, 7));								// Store Default Store
		modules.add(makePageModule(conf.getSfsId(), "85178146591D4FDF894ABD1AA1BFC403", null, 1, 3));								// Start From Scratch Start From Scratch Portlet
		modules.add(makePageModule(conf.getECommId(), "C69EA22EDCA047A8B3DAA6423961D4D8", null, 1, 5));								// Store Default Store
		modules.add(makePageModule(conf.getStoreId(), "0963E3463ED04E388A8F624F5FFA4DA9", null, 1, 1));								// Checkout Default Checkout
		modules.add(makePageModule(conf.getECommId(), "7f0001012bb130e8720775062d42eba0", null, 2, 5));								// Store Default Store
		
	}
	
	public Map<String, Integer> makeRoles() {
		Map<String, Integer> roles = new HashMap<String, Integer>();
		roles.put("0", 0);
		roles.put("10", 10);
		roles.put(conf.getRegisteredRoleId(), 30);  // the "Franchise" role
		roles.put("100", 100);
		roles.put("1000", 1000);
		return roles;
	}
	
	public Map<String, Integer> makeRoles(boolean isPrivate) {
		Map<String, Integer> roles = new HashMap<String, Integer>();
		if(!isPrivate){
		roles.put("0", 0);
		}
		else {
		roles.put("10", 10);
		roles.put(conf.getRegisteredRoleId(), 30);  // the "Franchise" role
		roles.put("100", 100);
		roles.put("1000", 1000);
		}
		return roles;
	}

	public PageModuleVO makePageModule(String actionId, String displayPgId, String paramNm, int col, int order) {
		PageModuleVO pm = new PageModuleVO();
		pm.setActionId(actionId);
		pm.setModuleDisplayId(displayPgId);
		pm.setParamName(paramNm);
		pm.setDisplayColumn(col);
		pm.setDisplayOrder(order);
		pm.setRoles(makeRoles()); //add default Public roles, some modules will override this
		return pm;
	}
	
	public PageModuleVO makePageModule(String actionId, String displayPgId, String paramNm, int col, int order, boolean isPrivate) {
		PageModuleVO pm = new PageModuleVO();
		pm.setActionId(actionId);
		pm.setModuleDisplayId(displayPgId);
		pm.setParamName(paramNm);
		pm.setDisplayColumn(col);
		pm.setDisplayOrder(order);
		pm.setRoles(makeRoles(isPrivate)); //add default Public roles, some modules will override this
		return pm;
	}

	/**
	 * Build the Ecommerce Template and return the id used to make it.
	 * @param req
	 * @return
	 */
	public String makeEcommTemplate(SMTServletRequest req) {
		String templateId = new UUIDGenerator().getUUID();
		String franId = CenterPageAction.getFranchiseId(req);
		StringBuilder sql = new StringBuilder();
		sql.append("insert into TEMPLATE (TEMPLATE_ID, SITE_ID, LAYOUT_NM, ");
		sql.append("DEFAULT_FLG, COLUMNS_NO, DEFAULT_COLUMN_NO, PRINTER_FRIENDLY_FLG, ");
		sql.append("EMAIL_FRIEND_FLG, META_KEYWORD_TXT, META_DESC_TXT, PAGE_TITLE_NM, ");
		sql.append("CREATE_DT) values(?,?,?,?,?,?,?,?,?,?,?,?)");
		PreparedStatement ps = null;
		try {
			int i = 1;
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(i++, templateId);
			ps.setString(i++, conf.getOrgPrefix() + franId + "_1");
			ps.setString(i++, "eStore Layout");
			ps.setInt(i++, 0);
			ps.setInt(i++, 2);
			ps.setInt(i++, 2);
			ps.setInt(i++, 0);
			ps.setInt(i++, 0);
			ps.setString(i++, "");
			ps.setString(i++, "");
			ps.setString(i++, "eStore Layout");
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("Could not create eComm Template", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}
		return templateId;
	}
}
