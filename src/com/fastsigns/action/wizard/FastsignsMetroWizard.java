package com.fastsigns.action.wizard;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.admin.action.PageModuleAction;
import com.smt.sitebuilder.admin.action.PageModuleRoleAction;
import com.smt.sitebuilder.admin.action.PageRoleAction;

/****************************************************************************
 * <b>Title</b>: FastsignsMetroWizard.java<p/>
 * <b>Description: Makes sure that there is a page and display module for metro
 * areas that are created, updates them when needed, and deletes them along
 * with the metro when it goes.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since March 20, 2014
 ****************************************************************************/

public class FastsignsMetroWizard  extends SBActionAdapter {

	// These values are set in thesetOrgValues method and are used to allow this to
	// work with all the Fastsigns locations.
	private String displayId;
	private String frachiseRoleId;
	private String templateId;

	/**
	 * Instantiates this metro wizard under the assumption we are creating a new metro area
	 * @param actionInit
	 * @param dbConn 
	 */
	public FastsignsMetroWizard(ActionInitVO actionInit, SMTDBConnection dbConn) {
		super(actionInit);
		this.dbConn = dbConn;
	}

	/**
	 * Create and update the page and module related to 
	 * the metro area that was just added or updated
	 * @param req
	 * @param metroAreaId
	 * @param newMetro
	 * @throws SQLException 
	 * @throws ActionException 
	 */
	public void updateMetroArea(SMTServletRequest req, String metroAreaId, boolean newMetro) throws ActionException, SQLException {
		setOrgValues(req.getParameter("organizationId"));
		updateMetroPage(req, metroAreaId, newMetro);
		updateMetroModule(req, metroAreaId, newMetro);
		if(newMetro)
			createPageModule(req, metroAreaId);
	}

	/**
	 * Set the organization specific values for templates and franchise role ids
	 * @param orgId
	 */
	private void setOrgValues(String orgId) {
		switch (orgId){
			case "FTS":
				frachiseRoleId = "c0a80167d141d17e20e2d7784364ab3f";
				templateId = "c0a8022d1322e2d32fa0b3c9a8fc9a85";
				break;
			case "FTS_AU":
				frachiseRoleId = "0a0014137c774a3967a8fbce2d42aabd";
				templateId = "0a0014137c7c92b3492ce5c68b5472b4";
				break;
			case "FTS_SA":
				frachiseRoleId = "0a001413d9796dc838f6a6115d19575a";
				templateId = "0a001413d97ca12538f6a611c9ad37ad";
				break;
			case "FTS_UK":
				frachiseRoleId = "0a00141332afab61abff2da59568338d";
				templateId = "0a00141332b1d797702bbfe38b1b5592";
				break;
		}
		displayId = "c0a8022318faf112d3061b3ba24abaff";
	}

	/**
	 * Creates or updates a page for the provided metro area
	 * @param req
	 * @param newMetro
	 * @throws SQLException
	 * @throws ActionException 
	 */
	private void updateMetroPage(SMTServletRequest req, String metroAreaId, 
			boolean newMetro) throws SQLException, ActionException {
		StringBuilder sql = createPageSQL(newMetro);
		String siteId = req.getParameter("organizationId") + "_7";

		PreparedStatement ps = dbConn.prepareStatement(sql.toString());

		ps.setString(1, "");
		ps.setString(2, templateId);
		ps.setString(3, siteId);
		ps.setString(4, req.getParameter("areaAlias"));
		ps.setString(5, req.getParameter("areaName") + " Metro Area");
		ps.setString(6, req.getParameter("title"));
		ps.setTimestamp(7, Convert.getCurrentTimestamp());
		ps.setInt(8, 100);
		ps.setInt(9, 0);
		ps.setInt(10, 0);
		ps.setTimestamp(11, Convert.getCurrentTimestamp());
		ps.setString(12, req.getParameter("metaKeyword"));
		ps.setString(13, req.getParameter("metaDesc"));
		ps.setInt(14, 0);
		ps.setString(15, "/");
		ps.setString(16, "/"+req.getParameter("areaAlias"));
		ps.setString(17, "");
		ps.setString(18, metroAreaId);
		int resultCount = ps.executeUpdate();
		ps.close();
		if(resultCount == 0) {
			throw new SQLException();
		}

		if (newMetro) {
			req.setAttribute("pageId", metroAreaId);
			req.setParameter("roles", new String[]{"0", "10", "100", frachiseRoleId}, true);
			SMTActionInterface aac = new PageRoleAction(this.actionInit);
			aac.setDBConnection(dbConn);
			aac.update(req);
		}
	}

	/**
	 * Build the sql for page creation/update
	 * @param newMetro
	 * @return
	 */
	private StringBuilder createPageSQL(boolean newMetro) {
		StringBuilder sql = new StringBuilder();

		if (newMetro) {
			sql.append("INSERT INTO PAGE (");
			sql.append("PARENT_ID, TEMPLATE_ID, SITE_ID, PAGE_ALIAS_NM, ");
			sql.append("PAGE_DISPLAY_NM, PAGE_TITLE_NM, LIVE_START_DT, ORDER_NO, VISIBLE_FLG, ");
			sql.append("FOOTER_FLG, CREATE_DT, META_KEYWORD_TXT, META_DESC_TXT, DEFAULT_FLG, ");
			sql.append("PARENT_PATH_TXT, FULL_PATH_TXT, CANONICAL_MOBILE_URL, PAGE_ID) ");
			sql.append("VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		} else {
			sql.append("update page set PARENT_ID = ?, TEMPLATE_ID = ?, SITE_ID = ?, PAGE_ALIAS_NM = ?, ");
			sql.append("PAGE_DISPLAY_NM = ?, PAGE_TITLE_NM = ?, LIVE_START_DT = ?, ORDER_NO = ?, ");
			sql.append("VISIBLE_FLG = ?, FOOTER_FLG = ?, UPDATE_DT = ?, META_KEYWORD_TXT = ?, META_DESC_TXT = ?, ");
			sql.append("DEFAULT_FLG = ?, PARENT_PATH_TXT = ?, FULL_PATH_TXT = ?, CANONICAL_MOBILE_URL = ? ");
			sql.append("WHERE PAGE_ID = ?");
		}
		return sql;
	}

	/**
	 * Creates or updates a Fastsigns Metro Display module for the provided metro area
	 * @param req
	 * @param metroAreaId
	 * @param newMetro
	 * @throws SQLException
	 */
	private void updateMetroModule(SMTServletRequest req, String metroAreaId, 
			boolean newMetro) throws SQLException {
		StringBuilder sql = createModuleSQL(newMetro);

		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setString(1, req.getParameter("organizationId"));
		ps.setString(2, "FTS_METRO");
		ps.setString(3, req.getParameter("areaName") + " Metro Area Display");
		ps.setString(4, "Display information for the " + req.getParameter("areaName") + " Metro Area");
		ps.setTimestamp(5, Convert.getCurrentTimestamp());
		ps.setString(6, metroAreaId);
		ps.setString(7, metroAreaId);

		int resultCount = ps.executeUpdate();
		ps.close();
		if(resultCount == 0) {
			throw new SQLException();
		}
	}

	/**
	 * Creates the sql for creating the metro display module
	 * @param newMetro
	 * @return
	 */
	private StringBuilder createModuleSQL(boolean newMetro) {
		StringBuilder sql = new StringBuilder();

		if (newMetro) {
			sql.append("INSERT INTO SB_ACTION (");
			sql.append("ORGANIZATION_ID, MODULE_TYPE_ID, ACTION_NM, ");
			sql.append("ACTION_DESC, CREATE_DT, ATTRIB1_TXT, ACTION_ID) ");
			sql.append("VALUES (?,?,?,?,?,?,?)");
		} else {
			sql.append("UPDATE SB_ACTION SET ");
			sql.append("ORGANIZATION_ID = ?, MODULE_TYPE_ID = ?, ACTION_NM = ?, ");
			sql.append("ACTION_DESC = ?, UPDATE_DT = ?, ATTRIB1_TXT = ? ");
			sql.append("WHERE ACTION_ID = ?");
		}
		return sql;
	}

	/**
	 * Puts the new display module on the new page
	 * @param req
	 * @param metroAreaId
	 * @param newMetro
	 * @throws ActionException 
	 */
	private void createPageModule(SMTServletRequest req, String metroAreaId) throws SQLException, ActionException {
		StringBuilder sql = createPageModuleSQL();

		PreparedStatement ps = dbConn.prepareStatement(sql.toString());

		ps.setString(1, metroAreaId);
		ps.setString(2, displayId);
		ps.setString(3, metroAreaId);
		ps.setString(4, metroAreaId);
		ps.setInt(5, 1);
		ps.setInt(6, 1);
		ps.setString(7, req.getParameter("areaName") + " Metro Area Display");
		ps.setTimestamp(8, Convert.getCurrentTimestamp());

		int resultCount = ps.executeUpdate();
		ps.close();
		if(resultCount == 0) {
			log.debug("something");
			throw new SQLException();
		}


		req.setAttribute("pageModuleId", metroAreaId);
		req.setParameter("roleId", new String[]{"0", "10", "100", frachiseRoleId}, true);
		PageModuleRoleAction pmra = new PageModuleRoleAction(actionInit);
		pmra.setDBConnection(dbConn);
		pmra.update(req);
	}

	/**
	 * Create the page module sql
	 * @return
	 */
	private StringBuilder createPageModuleSQL() {
		StringBuilder sql = new StringBuilder();

		sql.append("INSERT INTO PAGE_MODULE (");
		sql.append("PAGE_MODULE_ID, MODULE_DISPLAY_ID, PAGE_ID, ");
		sql.append("ACTION_ID, DISPLAY_COLUMN_NO, ORDER_NO, ");
		sql.append("MODULE_ACTION_NM, CREATE_DT) ");
		sql.append("VALUES (?,?,?,?,?,?,?,?)");

		return sql;
	}

	/**
	 * Delete the page and module that were created for this metro area.
	 */
	public void delete(SMTServletRequest req) throws ActionException {
		req.setParameter("skipApproval", "true");

		String id = req.getParameter("metroAreaId");
		String siteId = req.getParameter("organizationId") + "_7";

		try {
			deletePage(req, id, siteId);
			deleteModule(req, id);
		} catch (DatabaseException | SQLException e) {
			throw new ActionException(e.getMessage(), e.getCause());
		}
	}

	/**
	 * Deletes the page the associated with the metro area.
	 * @param req
	 * @throws ActionException 
	 */
	private void deletePage(SMTServletRequest req, String pageId, String SiteId) throws ActionException, SQLException {
		log.debug("Beginning Page Delete");
		StringBuilder sql = new StringBuilder("delete from page where page_id = ? or parent_id = ? ");

		PreparedStatement ps = null;

		ps = dbConn.prepareStatement(sql.toString());
		ps.setString(1, pageId);
		ps.setString(2, pageId); //delete all child pages too.  - JM 08/22/11

		// Make sure the update was successful
		int resultCount = ps.executeUpdate();
		ps.close();
		if(resultCount == 0) {
			throw new SQLException();
		}

		// Update the menu cache
		super.clearCacheByGroup(req.getParameter("siteId"));
		super.clearCacheByGroup(pageId);
	}

	/**
	 * Delete the module associated with this metro area
	 * @param req
	 * @param pageId
	 * @param SiteId
	 * @throws ActionException
	 * @throws SQLException
	 * @throws DatabaseException 
	 */
	private void deleteModule(SMTServletRequest req, String sbActionId) 
			throws ActionException, SQLException, DatabaseException {
		log.debug("Beginning Module Delete");
		StringBuilder sb = new StringBuilder();

		sb.append("delete from sb_action where action_id = ?");

		log.info("Delete SB Action SQL: " + sb + " - " + sbActionId);
		PreparedStatement ps = null;

		ps = dbConn.prepareStatement(sb.toString());
		ps.setString(1, sbActionId);

		// Make sure the update was successful
		int resultCount = ps.executeUpdate();
		ps.close();
		if(resultCount == 0) {
			throw new SQLException();
		}

		PageModuleAction aac = new PageModuleAction(this.actionInit); 
		aac.setDBConnection(dbConn);
		aac.delete(req, sbActionId);

		this.clearCacheByActionId(sbActionId);
	}
}
