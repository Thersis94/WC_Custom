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
		//List of media profiles stored for a franchise
		String [] mediaTypes = {"facebook_url","twitter_url","linkedin_url",
				"pinterest_url", "google_plus_url"};
		
		StringBuilder sql = new StringBuilder(220);
		//Using location url here, since only the corp sites are using the canonicals 
		sql.append("select dl.LOCATION_NM, dl.WEBSITE_URL, ");
		for (int index=0; index < mediaTypes.length; index++){
			sql.append(mediaTypes[index]);
			if (index+1 >= mediaTypes.length)
				sql.append(" ");
			else
				sql.append(",");
		}
		sql.append("from ").append(customDb).append("FTS_FRANCHISE ff ");
		sql.append("inner join DEALER_LOCATION dl on dl.DEALER_LOCATION_ID=ff.FRANCHISE_ID ");
		sql.append("where FRANCHISE_ID = ?");
		log.debug(sql.toString() + " | "+fId);
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i = 0;
			ps.setString(++i, fId);
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()){
				String tmp = null;
				for (String s : mediaTypes){
					tmp = StringUtil.checkVal(rs.getString(s));
					if (tmp.length() > 0)
						urlList.add(tmp);
				}
				vo.setAttribute("orgName", "FASTSIGNS of "+rs.getString("location_nm"));
				vo.setAttribute(MAIN_URL_PARAM, rs.getString("website_url"));
				vo.setAttribute(URL_PARAM, urlList);
			}
		}
	}

}
