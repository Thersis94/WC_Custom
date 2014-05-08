package com.fastsigns.security;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.common.SiteVO;


/****************************************************************************
 * <b>Title</b>: FsFranchiseRoleAction.java<p/>
 * <b>Description: Utility action to manage a user's franchise role XR record</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.5
 * @since Jan 05, 2011
 * JM - 03-22-11 - added support for users managing multiple franchises 
 * 				   (1->n relationship between FTS_FRANCHISE and PROFILE)
 ****************************************************************************/
public class FsFranchiseRoleAction extends SBActionAdapter {
	
	public static final String MULTI_FRAN_IDS = "franchiseIds";
	public static final String FRANCHISE_MAP = "franchiseMap";
	
	public FsFranchiseRoleAction() {
	}
	
	public FsFranchiseRoleAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void build(SMTServletRequest req) throws ActionException {	
		//try to retrieve user's data from attributes (login operation)
		String profileId = (String)attributes.get("profileId");
		log.debug("profileId from attributes: " + profileId);
		String countryCd = ((SiteVO)req.getAttribute("siteData")).getCountryCode();
		if (profileId == null) {
			//try to get user profileId from session (registration operation)
			UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
			if (user != null) {
				profileId = user.getProfileId();
				log.debug("profileId from user data: " + profileId);
			}
			if (StringUtil.checkVal(profileId).length() == 0) return;
		}
		
		final String schema = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		String dealerLocId = StringUtil.checkVal(req.getParameter("dealerLocationId"));
		
		// retrieve existing XR record for this user		
		String[] locns = this.checkFranchiseXR(profileId, schema, countryCd);
		String currLocId = (locns.length >= 1) ? locns[0] : null;
		
		log.debug("dealerLocationId: " + dealerLocId);
		log.debug("currLocId: " + currLocId);
		boolean isUpdate = (currLocId != null);

		//determine if the user is changing "their" center.  set the new value if so
		if (dealerLocId.length() > 0 && !dealerLocId.equalsIgnoreCase(currLocId)) {
			//update XR record with dealer loc id
			this.updateFranchiseXR(profileId, dealerLocId, isUpdate, schema, countryCd);
			currLocId = dealerLocId;
		}
		
		// If the user has a center we redirect the user to their center instead of the main site.
		// We need to set this in the session because at this point it is the only place we will pull a redirect url from
		if(req.hasParameter("isLogin") && currLocId != null && !req.hasParameter("isEcomm")) req.getSession().setAttribute("destUrl", "/"+currLocId);
		
		//populate the session
		this.updateFranchiseSessionData(req, currLocId, schema);
		
		if (locns.length > 1) {
			//this scenario is typically used by Keystone users -- Franchisees who own multiple stores
			//we performed all the default behaviors, now add the array of locn's to 
			//the session for Keystone to deal with.
			req.getSession().setAttribute(MULTI_FRAN_IDS, locns);
		}
	}
	
	/* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		this.build(req);
	}
	
	/**
	 * Checks for existing franchise XR record for user
	 * @param profileId
	 * @param schema
	 * @return
	 */
	private String[] checkFranchiseXR(String profileId, String schema, String countryCd) {
		List<String> franchiseId = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();
		sb.append("select franchise_id from ").append(schema);
		sb.append("fts_franchise_role_xr where profile_id = ? ");
		sb.append("and country_cd = ?");
		log.debug(sb);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, profileId);
			ps.setString(2, countryCd);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				franchiseId.add(rs.getString(1));

		} catch (SQLException sqle) {
			log.error("Error retrieving franchiseId XR record for user, ", sqle);
		}
		return franchiseId.toArray(new String[] {});
	}
	
	/**
	 * Inserts or updates franchise ID XR record for user
	 * @param profileId
	 * @param franchiseId
	 * @param update
	 * @param dbSchema
	 */
	public void updateFranchiseXR(String profileId, String franchiseId, boolean update, String schema, String country) {
		StringBuilder sb = new StringBuilder();
		if (update) {
			sb.append("update ").append(schema).append("fts_franchise_role_xr ");
			sb.append("set franchise_id = ? , update_dt = ? where profile_id = ? and country_cd = ?");
		} else {
			sb.append("insert into ").append(schema).append("fts_franchise_role_xr ");
			sb.append("(FRANCHISE_ID, CREATE_DT, PROFILE_ID, COUNTRY_CD, FTS_FRANCHISE_ROLE_XR_ID) ");
			sb.append("values (?,?,?,?,?)");
		}
		log.debug(sb);

		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, franchiseId);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, profileId);
			ps.setString(4, country);
			if (!update){
				ps.setString(5, new UUIDGenerator().getUUID());
				
			}
			ps.executeUpdate();

		} catch (SQLException sqle) {
			log.error("Error inserting or updating franchiseId XR record for user, ", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		log.debug("set _xr for pro=" + profileId + " and fran=" + franchiseId);
	}
	
	/**
	 * Removes/adds franchise ID, franchise alias ID from session
	 * @param req
	 * @param franchiseId
	 */
	private void updateFranchiseSessionData(SMTServletRequest req, String franchiseId, String schema) {
		//remove any existing franchise id data
		SiteVO site = (SiteVO)req.getAttribute("siteData");
		req.getSession().removeAttribute("FranchiseId");
		req.getSession().removeAttribute("FranchiseAliasId");
		
		if (franchiseId != null) {
			// if we have inserted or updated an XR record, query for the current info
			// in order to set the appropriate session vars
			StringBuffer sb = new StringBuffer();
			
			sb.append("select b.franchise_id, c.location_alias_nm, c.location_nm from ");
			sb.append(schema).append("fts_franchise a inner join ").append(schema);
			sb.append("fts_franchise_role_xr b on a.franchise_id = b.franchise_id ");
			sb.append("inner join dealer_location c on b.franchise_id = c.dealer_location_id ");
			sb.append("where b.franchise_id = ? ");
			sb.append("group by b.franchise_id, c.location_alias_nm, c.location_nm ");
			
			log.debug(sb);
			PreparedStatement ps = null;
			try {
				ps = dbConn.prepareStatement(sb.toString());
				ps.setString(1, franchiseId);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					Map<String, Object> franchiseMap = new HashMap<String, Object>();
					
					//add franchise data to the map
					franchiseMap.put("FranchiseId", rs.getString(1));
					franchiseMap.put("FranchiseAliasId", rs.getString(2));
					franchiseMap.put("FranchiseLocationName", rs.getString(3));
					
					//set session values
					if(site.getAliasPathName() != null && site.getAliasPathName().equals("webedit")){
						req.getSession().setAttribute("webeditFranId", rs.getString(1));
						req.getSession().setAttribute("webeditFranAliasId", rs.getString(2));

					}
					else{
						req.getSession().setAttribute("FranchiseId", rs.getString(1));		
						req.getSession().setAttribute("FranchiseAliasId", rs.getString(2));
					}

					// set map on request for retrieval upstream
					req.setAttribute(FRANCHISE_MAP, franchiseMap);
				}
			} catch (SQLException sqle) {
				log.error("Error retrieving franchiseId XR record for user, ", sqle);
			}
		}
	}
}
