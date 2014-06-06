package com.fastsigns.action.wizard;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fastsigns.action.franchise.CenterPageAction;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>FastsignsClosingWizard.java<p/>
 * <b>Description: Automatically closes down a center, removes their site from 
 * the parent, and puts in redirects to handle all the removed pages.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since June 6, 2014
 * <b>Changes: </b>
 ****************************************************************************/

public class FastsignsClosingWizard extends SBActionAdapter {
	
	/**
	 * Get a list of all the pages on the main and mobile site for this center
	 */
	public void retrieve (SMTServletRequest req) {log.debug(((SiteVO)req.getAttribute("siteData")).getCountryCode());
		String franchiseId = CenterPageAction.getFranchiseId(req);
		String orgId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId() + "_" + franchiseId;
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ALIAS_PATH_NM, FULL_PATH_TXT, s.site_id, p.default_flg FROM PAGE p ");
		sql.append("left join SITE s on p.SITE_ID = s.SITE_ID ");
		sql.append("WHERE p.SITE_ID in (?,?)");
		
		Map <String, List<String>> urlMap = new HashMap<String, List<String>>();
		List<String> desktopList = new ArrayList<String>();
		List<String> mobileList = new ArrayList<String>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());

			ps.setString(1, orgId + "_1");
			ps.setString(2, orgId + "_2");
			
			rs = ps.executeQuery();
			
			// Put all the pages into the right page lists based on thier site id
			while (rs.next()) {
				if ((orgId+"_1").equals(rs.getString(3))) {
					if (rs.getInt(4) > 0) {
						desktopList.add(rs.getString(1));
					} else {
						desktopList.add(rs.getString(1)+rs.getString(2));
					}
				} else {
					if (rs.getInt(4) > 0) {
						mobileList.add(rs.getString(1));
					} else {
						mobileList.add(rs.getString(1)+rs.getString(2));
					}
				}
			}
			
			req.setParameter("activeDealer", String.valueOf(isLocationActive(franchiseId)));
		} catch (SQLException e) {
			log.error("Could not retrieve pages for franchise " + franchiseId, e);
		} finally {
			try {
				ps.close();
				rs.close();
			} catch (SQLException e) {}
		}

		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		urlMap.put("Desktop Site Urls", desktopList);
		if (mobileList.size() > 0)
			urlMap.put("Mobile Site Urls", mobileList);
		mod.setActionData(urlMap);
		attributes.put(Constants.MODULE_DATA, mod);
		
	}
	
	/**
	 * Check whether the location is currently active or not in order to determine if it has to be closed down or opened
	 * @param franchiseId
	 * @return
	 */
	private int isLocationActive(String franchiseId) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ACTIVE_FLG FROM DEALER_LOCATION WHERE DEALER_LOCATION_ID = ?");
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, franchiseId);
			
			rs = ps.executeQuery();
			
			if(rs.next())
				return rs.getInt(1);
			
		} catch (SQLException e) {
			log.error("Could not determine if franchise " + franchiseId + "was open. ", e);
		} finally {
			try {
				ps.close();
				rs.close();
			} catch (SQLException e) {}
		}
		
		
		return 0;
	}
		
	public void build (SMTServletRequest req) {
		String orgId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();
		String franchiseId = CenterPageAction.getFranchiseId(req);
		
		//Determine if we are closing or opening a center
		if (Convert.formatBoolean(req.getParameter("open")))
			openCenter(req, orgId, franchiseId);
		else
			closeCenter(req, orgId, franchiseId);

		// Clear the cache of the parent site and the child sites so users don't get sent where they shouldn't be
		super.clearCacheByGroup(orgId+"_7");
		super.clearCacheByGroup(orgId+"_4");
		super.clearCacheByGroup(orgId+"_"+franchiseId+"_1");
		super.clearCacheByGroup(orgId+"_"+franchiseId+"_2");
	}
	
	/**
	 * Run through the closing process of adding redirects, 
	 * removing the alias, and shutting down the center
	 * @param req
	 * @param orgId
	 * @param franchiseId
	 */
	private void closeCenter (SMTServletRequest req, String orgId, String franchiseId) {		
		//Get the list of redirects and add them to the database
		Map<String, Map<String, String>> redirectMap = parseRedirects(req, orgId);
		createRedirects(redirectMap, franchiseId);
		
		// Remove the site from its parent
		modifyAlias(orgId+ "_" + franchiseId + "_1", "", "");
		modifyAlias(orgId+ "_" + franchiseId + "_2", "", "");
		
		// Remove the location from the locator
		modifyLocation(franchiseId, 0);
		
		req.setParameter("resultMsg", "You have succesfully closed down this center.");
	}
	
	/**
	 * Get a list of all the redirects that the user entered for the pages on the site
	 * @param req
	 * @param orgId
	 * @return
	 */
	private Map<String, Map<String, String>> parseRedirects (SMTServletRequest req, String orgId) {
		Map<String, Map<String, String>> fullMap = new HashMap<String, Map<String, String>>();
		Map<String, String> desktopMap = new HashMap<String, String>();
		Map<String, String> mobileMap = new HashMap<String, String>();
		int i = 1;
		
		// Get all the desktop urls
		while(StringUtil.checkVal(req.getParameter("redirectUrl_"+i)).length() > 0) {
			desktopMap.put(StringUtil.checkVal(req.getParameter("redirectUrl_" + i)),
					StringUtil.checkVal(req.getParameter("destinationUrl_" + i)));
			i++;
		}
		
		// Reset i and get all the mobile urls
		i = 1;
		while(StringUtil.checkVal(req.getParameter("mobileRedirectUrl_"+i)).length() > 0) {
			mobileMap.put(StringUtil.checkVal(req.getParameter("mobileRedirectUrl_" + i)),
					StringUtil.checkVal(req.getParameter("mobileDestinationUrl_" + i)));
			i++;
		}

		fullMap.put(orgId + "_7", desktopMap);
		fullMap.put(orgId + "_4", mobileMap);
		
		return fullMap;
	}

	/**
	 * Create the redirects we retrieved from the form.
	 * These are all given a similar id for ease of deletion later
	 * @param fullMap
	 * @param franchiseId
	 */
	private void createRedirects (Map<String, Map<String, String>> fullMap, String franchiseId) {
		StringBuilder sql = new StringBuilder();
		
		sql.append("INSERT INTO SITE_REDIRECT (SITE_REDIRECT_ID, SITE_ID, REDIRECT_ALIAS_TXT, DESTINATION_URL, ");
		sql.append("ACTIVE_FLG, CREATE_DT, GLOBAL_FLG, PERMANENT_REDIR_FLG, LOG_REDIR_FLG) ");
		sql.append("VALUES(?,?,?,?,1,?,0,0,0)");
		
		int i = 0;
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			for (String siteId: fullMap.keySet()) {
				for (String key : fullMap.get(siteId).keySet()) {
					ps.setString(1,franchiseId + "_closing_redirect_" + i++);
					ps.setString(2, siteId);
					ps.setString(3, key);
					ps.setString(4, fullMap.get(siteId).get(key));
					ps.setTimestamp(5, Convert.getCurrentTimestamp());
					ps.addBatch();
				}
			}
			
			ps.executeBatch();
			
		} catch (SQLException e) {
			log.error("Unable to add redirects for center " + franchiseId, e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {}
		}
	}
	
	/**
	 * Change the alias and parentId of the site to whatever
	 * is being given to the method.  Can either close or open
	 * the center
	 * @param siteId
	 * @param parentId
	 * @param newAlias
	 */
	private void modifyAlias (String siteId, String parentId, String newAlias) {
		StringBuilder sql = new StringBuilder();
		
		sql.append("UPDATE SITE SET ALIAS_PATH_NM = ?, ALIAS_PATH_PARENT_ID = ? WHERE SITE_ID = ?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			
			ps.setString(1, newAlias);
			ps.setString(2, parentId);
			ps.setString(3, siteId);
			
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Unable to update alias for site " + siteId, e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {}
		}
	}
	
	/**
	 * Change the activeFlg of the chosen center, either removing or
	 * adding it to the list of active centers
	 * @param franchiseId
	 * @param activeFlg
	 */
	private void modifyLocation(String franchiseId, int activeFlg) {
		StringBuilder sql = new StringBuilder();
		
		sql.append("UPDATE DEALER_LOCATION SET ACTIVE_FLG = ? WHERE DEALER_LOCATION_ID = ?");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			
			ps.setInt(1, activeFlg);
			ps.setString(2, franchiseId);
			
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Unable to update dealer with id " + franchiseId, e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {}
		}
	}
	
	/**
	 * Go through the process of deleting redirects, reactivating the
	 * location, and adding it to the main site
	 * @param req
	 * @param orgId
	 * @param franchiseId
	 */
	private void openCenter (SMTServletRequest req, String orgId, String franchiseId) {
		String newAlias = StringUtil.checkVal(req.getParameter("newAlias"));
		
		deleteRedirects(franchiseId);
		modifyAlias( orgId + "_" + franchiseId + "_1", orgId + "_7", newAlias);
		modifyAlias( orgId + "_" + franchiseId + "_2", orgId + "_4", newAlias);
		modifyLocation(franchiseId, 1);
		req.setParameter("resultMsg", "You have succesfully reopened this center.");
	}
	
	/**
	 * Delete all the redirects that were created by closing down the center
	 * @param siteId
	 */
	private void deleteRedirects (String franchiseId) {
		StringBuilder sql = new StringBuilder();
		
		sql.append("DELETE FROM SITE_REDIRECT WHERE SITE_REDIRECT_ID like ?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, franchiseId + "_closing_redirect%");
			
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Unable to delete redirects in regarding franchise " + franchiseId, e);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {}
		}
	}

}
