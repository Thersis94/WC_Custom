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
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
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
	public static final String MODULE_NAME = "FTS_PROFILE_MAP";
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
		
		if (actionData == null)
			actionData = new SBModuleVO();
		
		try {
			getMediaLinks(req, actionData);
		} catch (SQLException e) {
			log.error("Unable to retrieve media info:",e);
			return;
		}
		this.putModuleData(actionData);
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
		sql.append("left join SB_ACTION sb on sb.MODULE_TYPE_ID = ? ");
		sql.append("and sb.ORGANIZATION_ID=d.ORGANIZATION_ID+'_'+ff.FRANCHISE_ID ");
		sql.append("where FRANCHISE_ID = ?");
		log.debug(sql.toString() + " | "+fId);
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i = 0;
			ps.setString(++i, MODULE_NAME);
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
		
		StringBuilder sql = new StringBuilder(80);
		//Only one graph per center, so orgId+modtype is fine
		sql.append("delete from SB_ACTION where ORGANIZATION_ID = ? and MODULE_TYPE_ID = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i = 0;
			deleteFromLayout(foId);
			
			log.debug(sql.toString()+" | "+foId+" | "+MODULE_NAME);
			ps.setString(++i, foId);
			ps.setString(++i, MODULE_NAME);
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
			actionData.setActionId(MODULE_NAME);
			modVO.setActionData(actionData);
		}
		
		try {
			getMediaLinks(req, actionData);
		} catch (SQLException e) {
			log.error(e);
			throw new ActionException(e);
		}
		log.debug("Updating social profile info...");
		modVO.setActionId(MODULE_NAME);
		this.attributes.put(AdminConstants.ADMIN_MODULE_DATA, modVO);
		boolean isInsert = StringUtil.checkVal(req.getParameter(SB_ACTION_ID)).isEmpty();
		
		List<String> urlList = (List<String>) actionData.getAttribute(URL_PARAM);
		req.setParameter(URL_PARAM, urlList.toArray(new String[urlList.size()]), true);
		req.setParameter("organizationId", foId);
		req.setParameter("actionName", "Social Profile Map");
		req.setParameter("actionDesc", "Social Profile Map");
		req.setParameter(SB_ACTION_ID, actionData.getActionId());
		req.setParameter(SB_ACTION_GROUP_ID, actionData.getActionGroupId());
		
		try {
			super.update(req);
			if (isInsert) 
				addToLayout(foId, (String)req.getAttribute(SB_ACTION_ID));
		} catch (SQLException e) {
			log.error(e);
			throw new ActionException(e);
		}
	}
	
	/**
	 * Checks whether there was a change to the social media data for a center,
	 * and updates the profile map accordingly. Change depends on the existence 
	 * of the media link parameters on the request.
	 * @param req
	 * @param franchiseOrgId
	 * @throws ActionException
	 */
	public void updateFranchiseMap(SMTServletRequest req, String franchiseOrgId) throws ActionException{
		boolean hasLink = false;
		//if there are any social links present, we aren't deleting it
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
	
	/**
	 * Removes the profile map from the center's default layout
	 * @param franchiseOrgId
	 * @throws SQLException
	 */
	private void deleteFromLayout(String franchiseOrgId) throws SQLException{
		String siteId = franchiseOrgId+"_1";
		
		StringBuilder sql = new StringBuilder(500);
		sql.append("delete from PAGE_MODULE where PAGE_MODULE_ID in (");
		sql.append("select pm.PAGE_MODULE_ID from TEMPLATE t ");
		sql.append("inner join PAGE p on p.TEMPLATE_ID=t.TEMPLATE_ID and p.SITE_ID=? ");
		sql.append("inner join PAGE_MODULE pm on pm.TEMPLATE_ID=t.TEMPLATE_ID ");
		sql.append("inner join MODULE_DISPLAY md on md.MODULE_DISPLAY_ID=pm.MODULE_DISPLAY_ID ");
		sql.append("inner join MODULE_TYPE mt on mt.MODULE_TYPE_ID=? and mt.MODULE_TYPE_ID=md.MODULE_TYPE_ID ");
		sql.append("where t.DEFAULT_FLG=1) ");
		
		log.debug(sql.toString()+" | "+siteId+" | "+MODULE_NAME);
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i=0;
			ps.setString(++i, siteId);
			ps.setString(++i, MODULE_NAME);
			int affected = ps.executeUpdate();
			log.debug(affected + " page modules removed from layout.");
		}
	}
	
	/**
	 * Adds the new action to the center's default layout
	 * @param franchiseOrgId
	 * @param actionId
	 * @throws SQLException
	 */
	private void addToLayout(String franchiseOrgId, String actionId) throws SQLException{
		String pKey = new UUIDGenerator().getUUID();
		String aId = StringUtil.checkVal(actionId, null);
		
		StringBuilder sql = new StringBuilder(590);
		sql.append("insert into PAGE_MODULE (PAGE_MODULE_ID, MODULE_DISPLAY_ID, ");
		sql.append("PAGE_ID, TEMPLATE_ID, ACTION_ID, DISPLAY_COLUMN_NO, ");
		sql.append("ORDER_NO, CREATE_DT ) ");
		sql.append("select TOP 1 ? as PAGE_MODULE_ID, md.MODULE_DISPLAY_ID, p.PAGE_ID, ");
		sql.append("t.TEMPLATE_ID, ? as ACTION_ID, ? as DISPLAY_COLUMN_NO, ");
		sql.append("? as ORDER_NO, ? as CREATE_DT from TEMPLATE t ");
		sql.append("inner join PAGE p on p.TEMPLATE_ID=t.TEMPLATE_ID and p.SITE_ID=? ");
		sql.append("left join MODULE_DISPLAY md on md.MODULE_TYPE_ID=? ");
		sql.append("where p.DEFAULT_FLG=1 ");
		log.debug(sql.toString()+" | "+pKey+" | "+actionId);
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i=0;
			ps.setString(++i, pKey);
			ps.setString(++i, aId);
			ps.setInt(++i, 1);
			ps.setInt(++i, 60);
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			ps.setString(++i, franchiseOrgId+"_1");
			ps.setString(++i, MODULE_NAME);
			ps.execute();
		}
	}
	
	private void addModuleRoles(String franchiseOrgId, String pmid) throws SQLException{
		String corpId = franchiseOrgId.split("_")[0];
		StringBuilder sql = new StringBuilder();
		sql.append("insert into PAGE_MODULE_ROLE (PAGE_MODULE_ROLE_ID, ROLE_ID, ");
		sql.append("PAGE_MODULE_ID, CREATE_DT) ");
		sql.append("select LOWER(REPLACE(NEWID(),'-','')), r.ROLE_ID, ?, ? from ROLE r ");
		sql.append("where r.ROLE_ID in ('0','10','100') or r.ORGANIZATION_ID=? ");
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			
		}
	}
	
	private void removeModuleRoles(String franchiseOrgId){
		
	}

}
