/**
 * 
 */
package com.fastsigns.action.franchise;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.tools.ProfileMapAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>:SocialProfileMapAction.java<p/>
 * <b>Description: Maps social media data to website so it appears in 
 * Google search results. This action is for the franchises, corp uses core 
 * module.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @version 1.0
 * @since Mar 30, 2015
 ****************************************************************************/
public class SocialProfileMapAction extends ProfileMapAction {

	public static final String MAIN_URL_PARAM = "mainUrl";
	public enum SocialMediaTypes{
		FACEBOOK("facebook_url","facebookUrl"),
		TWITTER("twitter_url","twitterUrl"),
		LINKEDIN("linkedin_url","linkedinUrl"),
		FOURSQUARE("foursquare_url","foursquareUrl"),
		PINTEREST("pinterest_url","pinterestUrl"),
		GOOGLE_PLUS("google_plus_url","googlePlusUrl");
		private String dbName;
		private String paramName;
		
		SocialMediaTypes(String dbName, String paramName){
			this.dbName = dbName;
			this.paramName = paramName;
		}
		public String getDbName(){ return dbName; }
		public String getParamName(){ return paramName; }
	}
	
	/**
	 * Default Constructor
	 */
	public SocialProfileMapAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public SocialProfileMapAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.tools.ProfileMapAction#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException{
		super.retrieve(req);
		
		ModuleVO modVO = (ModuleVO) this.getAttribute(Constants.MODULE_DATA);
		SBModuleVO actionData = (SBModuleVO) modVO.getActionData();
		
		if (actionData != null){
			try {
				getMediaLinks(req, actionData);
			} catch (SQLException e) {
				log.error("Unable to retrieve media info:",e);
				return;
			}
			this.putModuleData(actionData);
		}
	}
	
	/**
	 * Gets the social media links and org name from the db for a franchise, and
	 * sets it on the vo.
	 * @param req
	 * @param vo
	 * @return
	 * @throws SQLException
	 */
	protected void getMediaLinks(SMTServletRequest req, SBModuleVO vo) throws SQLException{
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		List<String> urlList = new ArrayList<String>();
		String fId = CenterPageAction.getFranchiseId(req);
		
		StringBuilder sql = new StringBuilder(220);
		//Using location url here, since only the corp sites are using the canonicals 
		sql.append("select dl.LOCATION_NM, dl.WEBSITE_URL, sb.ACTION_ID, sb.ACTION_GROUP_ID, ");
		SocialMediaTypes [] types = SocialMediaTypes.values();
		for (int index=0; index < types.length; index++){
			sql.append(types[index].getDbName());
			if (index+1 >= types.length)
				sql.append(" ");
			else
				sql.append(",");
		}
		sql.append("from ").append(customDb).append("FTS_FRANCHISE ff ");
		sql.append("inner join DEALER_LOCATION dl on dl.DEALER_LOCATION_ID=ff.FRANCHISE_ID ");
		sql.append("inner join DEALER d on d.DEALER_ID=dl.DEALER_ID ");
		sql.append("left join SB_ACTION sb on sb.MODULE_TYPE_ID='FTS_PROFILE_MAP' ");
		sql.append("and sb.ORGANIZATION_ID=d.ORGANIZATION_ID+'_'+ff.FRANCHISE_ID ");
		sql.append("where FRANCHISE_ID = ?");
		log.debug(sql.toString() + " | "+fId);
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i = 0;
			ps.setString(++i, fId);
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()){
				String tmp = null;
				for (SocialMediaTypes s : SocialMediaTypes.values()){
					tmp = StringUtil.checkVal(rs.getString(s.getDbName()));
					if (tmp.length() > 0)
						urlList.add(tmp);
				}
				vo.setAttribute("orgName", "FASTSIGNS of "+rs.getString("location_nm"));
				vo.setAttribute(MAIN_URL_PARAM, rs.getString("website_url"));
				vo.setAttribute(URL_PARAM, urlList);
				vo.setActionId(rs.getString("action_id"));
				vo.setActionGroupId(rs.getString("action_group_id"));
			}
		}
	}
	
	/**
	 * Delete the graph for a franchise
	 * @param franchiseOrgId The org id for that center (ex: FTS_123)
	 * @throws ActionException
	 */
	protected void deleteFranchiseMap(String franchiseOrgId) throws ActionException{
		String foId = StringUtil.checkVal(franchiseOrgId, null);
		if (foId == null){
			log.error("Missing franchise org id, unable to delete.");
			return;
		}
		
		final String modType = "FTS_PROFILE_MAP";
		StringBuilder sql = new StringBuilder(80);
		//Only one graph per center, so orgId+modtype is fine
		sql.append("delete from SB_ACTION where ORGANIZATION_ID = ? and MODULE_TYPE_ID = ? ");
		log.debug(sql.toString()+" | "+foId+" | "+modType);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i = 0;
			ps.setString(++i, foId);
			ps.setString(++i, modType);
			int affected = ps.executeUpdate();
			log.debug(affected+" record(s) removed.");
		} catch(SQLException e){
			log.error(e);
			throw new ActionException(e);
		}
	}
	
	/**
	 * Create a new profile map for this franchise
	 * @param req
	 * @param franchiseOrgId
	 * @throws ActionException 
	 */
	@SuppressWarnings("unchecked")
	protected void createFranchiseMap(SMTServletRequest req, String franchiseOrgId) throws ActionException{
		String foId = StringUtil.checkVal(franchiseOrgId, null);
		if (foId == null){
			log.error("Missing franchise org id, unable to create profile map.");
			return;
		}
		
		ModuleVO modVO = (ModuleVO) this.getAttribute(Constants.MODULE_DATA);
		SBModuleVO actionData = (SBModuleVO) modVO.getActionData();
		if (actionData == null){
			actionData = new SBModuleVO();
			actionData.setActionId("FTS_PROFILE_MAP");
			modVO.setActionData(actionData);
		}
		
		try {
			getMediaLinks(req, actionData);
		} catch (SQLException e) {
			log.error(e);
			throw new ActionException(e);
		}
		log.debug("Updating social profile info...");
		modVO.setActionId("FTS_PROFILE_MAP");
		this.attributes.put(AdminConstants.ADMIN_MODULE_DATA, modVO);
		
		List<String> urlList = (List<String>) actionData.getAttribute(URL_PARAM);
		req.setParameter(URL_PARAM, urlList.toArray(new String[urlList.size()]), true);
		req.setParameter("organizationId", franchiseOrgId);
		req.setParameter("actionName", "Social Profile Map");
		req.setParameter("actionDesc", "Social Profile Map");
		req.setParameter(SB_ACTION_ID, actionData.getActionId());
		req.setParameter(SB_ACTION_GROUP_ID, actionData.getActionGroupId());
		super.update(req);
	}
	
	public void updateFranchiseMap(SMTServletRequest req, String franchiseOrgId) throws ActionException{
		boolean hasLink = false;
		for (SocialMediaTypes s : SocialMediaTypes.values()){
			hasLink = req.hasParameter(s.getParamName());
			if (hasLink)
				break;
		}
		if (hasLink)
			createFranchiseMap(req, franchiseOrgId);
		else
			deleteFranchiseMap(franchiseOrgId);
	}

}
